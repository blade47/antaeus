package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import mu.KotlinLogging
import kotlin.random.Random

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val currencyProvider: CurrencyProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService,
    private val subscriptionService: SubscriptionService,
    private val planService: PlanService
) {
    private val logger = KotlinLogging.logger {}

    init {
        val customers = this.customerService.fetchAll()
        customers.forEach { customer ->
            try {
                val subscription: Subscription = this.createSubscription(customer)
                if ( this.invoiceSubscription(subscription) ) this.activateSubscription(subscription)
            } catch (e: Exception) {
                logger.error(e) { "Failed to handle subscription for customer ${customer.id}. $e" }
            }
        }
    }

    fun createSubscription(customer: Customer): Subscription {
        val randomPlanDescription = PlanDescription.values()[Random.nextInt(0, PlanDescription.values().size)]
        val plan: Plan = try { this.planService.getByDescription(randomPlanDescription) } catch (e: PlanNotFoundException) { planService.fetchFirst() }
        return this.subscriptionService.subscribe(customer, plan)
    }

    fun invoiceSubscription(subscription: Subscription): Boolean {

        val retryHandler = RetryOnException()

        while (true) {
            try {
                return this.charge(subscription)
            } catch (e: CustomerNotFoundException) {
                logger.error { "Failed to charge subscription fee for subscription ${subscription.id}. Customer not found." }
                cancelSubscription(subscription)
                return false
            } catch (e: NetworkException) {
                retryHandler.exceptionOccurred(e);
                logger.warn { "Network error while charging subscription fee for subscription ${subscription.id}. Retrying..." }
            } catch (e: InvalidCurrencyException) {
                logger.error(e) { "Failed to convert currency for subscription ${subscription.id}. $e" }
                cancelSubscription(subscription)
                return false
            } catch (e: Exception) {
                logger.error(e) { "Failed to charge subscription fee for subscription ${subscription.id}. $e" }
                cancelSubscription(subscription)
                return false
            }
        }
    }

    private fun cancelSubscription(subscription: Subscription) {
        logger.trace { "Cancelling subscription ${subscription.id}" }
        subscription.subscriptionStatus = SubscriptionStatus.CANCELED
        subscriptionService.update(subscription)
    }

    private fun activateSubscription(subscription: Subscription) {
        logger.trace { "Activating subscription ${subscription.id}" }
        subscription.subscriptionStatus = SubscriptionStatus.ACTIVE
        subscriptionService.update(subscription)
    }

    private fun subscriptionPastDue(subscription: Subscription) {
        logger.trace { "Subscription ${subscription.id} past due." }
        subscription.subscriptionStatus = SubscriptionStatus.PAST_DUE
        subscriptionService.update(subscription)
    }

    private fun subscriptionExpired(subscription: Subscription) {
        logger.trace { "Subscription ${subscription.id} expired." }
        subscription.subscriptionStatus = SubscriptionStatus.INCOMPLETE_EXPIRED
        subscriptionService.update(subscription)
    }

    private fun updateLatestInvoice(to: Subscription, with: Invoice) {
        logger.trace { "Updating latest invoice for subscription: ${to.id} with invoice: ${with.id}." }
        to.latestInvoiceId = with.id
        subscriptionService.update(to)
    }

    private fun charge(subscription: Subscription) : Boolean {
        val customer: Customer = this.customerService.fetch(subscription.customerId)

        val invoiceToCharge: Invoice = try {
            subscription.latestInvoiceId?.let {
                val latestInvoice = this.invoiceService.fetch(it)
                if (latestInvoice.status != InvoiceStatus.PENDING) {
                    val newInvoice = this.createNewInvoice(subscription)
                    this.updateLatestInvoice(to = subscription, with = newInvoice)
                    newInvoice
                } else {
                    latestInvoice
                }
            }?: run {
                val newInvoice = this.createNewInvoice(subscription)
                this.updateLatestInvoice(to = subscription, with = newInvoice)
                newInvoice
            }

        } catch (e: InvoiceNotFoundException) {
            logger.warn { "Failed to retrieve latest invoice for subscription ${subscription.id}." }
            val newInvoice = this.createNewInvoice(subscription)
            this.updateLatestInvoice(to = subscription, with = newInvoice)
            newInvoice
        }

        try {
            if ( ! this.paymentProvider.charge(invoiceToCharge) ) {
                logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Account balance did not allow the charge." }
                return false;
            }
            logger.info { "Customer ${customer.id} charged successfully for subscription ${subscription.id}." }
            this.validate(invoiceToCharge)
            return true;

        } catch (e: CurrencyMismatchException) {
            logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Currency mismatch." }
            val newInvoice = adjustCurrency(to = invoiceToCharge, accordingTo = customer)
            this.updateLatestInvoice(to = subscription, with = newInvoice)
            return charge(subscription = subscription)
        }
    }

    private fun validate(invoice: Invoice) {
        logger.trace { "Validating invoice ${invoice.id}" }
        invoice.status = InvoiceStatus.PAID
        invoiceService.update(invoice)
    }

    private fun invalidate(invoice: Invoice) {
        logger.trace { "Invalidating invoice ${invoice.id}" }
        invoice.status = InvoiceStatus.CANCELED
        invoiceService.update(invoice)
    }

    private fun adjustCurrency(to: Invoice, accordingTo: Customer) : Invoice {
        logger.info { "Creating new invoice with updated currency..." }

        val moneyInNewCurrency = convertCurrency(from = to.amount, to = accordingTo.currency)
        this.invalidate(to)
        return createInvoice(ofAmount = moneyInNewCurrency, to = accordingTo)
    }

    private fun createNewInvoice(subscription: Subscription) : Invoice {
        val plan: Plan = this.planService.fetch(subscription.planId)
        val to: Customer = this.customerService.fetch(subscription.customerId)
        return createInvoice(plan.amount, to)
    }

    private fun createInvoice(ofAmount: Money, to: Customer) : Invoice {
        return invoiceService.create(ofAmount, to)
    }

    private fun convertCurrency(from: Money, to: Currency) : Money {
        return currencyProvider.convert(from, to)
    }
}
