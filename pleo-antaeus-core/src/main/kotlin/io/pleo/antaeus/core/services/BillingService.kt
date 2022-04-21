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
                this.invoiceSubscription(subscription)
            } catch (e: Exception) {
                logger.error(e) { "Failed to handle subscription for customer ${customer.id}. $e" }
            }
        }
    }

    private fun createSubscription(customer: Customer): Subscription {
        val randomPlanDescription = PlanDescription.values()[Random.nextInt(0, PlanDescription.values().size)]
        val plan: Plan = try { this.planService.getByDescription(randomPlanDescription) } catch (e: PlanNotFoundException) { planService.fetchFirst() }
        return this.subscriptionService.subscribe(customer, plan)
    }

    private fun invoiceSubscription(subscription: Subscription): Boolean {

        val retryHandler = RetryOnException()

        while (true) {
            try {
                val succedeed = this.charge(subscription)
                if (succedeed) {
                    // Charged successfully
                } else {
                    // Handle failure
                }
            } catch (e: CustomerNotFoundException) {
                logger.warn { "Failed to charge subscription fee for subscription ${subscription.id}. Customer not found." }
                cancelSubscription(subscription)
            } catch (e: NetworkException) {
                retryHandler.exceptionOccurred(e);
                logger.warn { "Network error while charging subscription fee for subscription ${subscription.id}. Retrying..." }
            } catch (e: InvalidCurrencyException) {
                logger.error(e) { "Failed to convert currency for subscription ${subscription.id}. $e" }
                cancelSubscription(subscription)
            } catch (e: Exception) {
                logger.error(e) { "Failed to charge subscription fee for subscription ${subscription.id}. $e" }
                cancelSubscription(subscription)
            }
        }
    }

    private fun cancelSubscription(subscription: Subscription) {

    }

    private fun charge(subscription: Subscription) : Boolean {
        val customer: Customer = this.customerService.fetch(subscription.customerId)

        val invoiceToCharge: Invoice = try {
            val latestInvoice = this.invoiceService.fetch(subscription.latestInvoiceId)
            if (latestInvoice.status != InvoiceStatus.PENDING) {
                createNewInvoice(subscription)
            } else {
                latestInvoice
            }
        } catch (e: InvoiceNotFoundException) {
            logger.warn { "Failed to retrieve latest invoice for subscription ${subscription.id}." }
            createNewInvoice(subscription)
        }

        try {
            if ( ! this.paymentProvider.charge(invoiceToCharge) ) {
                logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Account balance did not allow the charge." }
                return false;
            }
            logger.info { "Customer ${customer.id} charged successfully." }
            return true;

        } catch (e: CurrencyMismatchException) {
            logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Currency mismatch." }
            val newInvoice = adjustCurrency(to = invoiceToCharge, accordingTo = customer)
            subscription.latestInvoiceId = newInvoice.id
            subscriptionService.update(subscription)
            return charge(subscription = subscription)
        }
    }

    private fun adjustCurrency(to: Invoice, accordingTo: Customer) : Invoice {
        logger.info { "Creating new invoice with updated currency..." }

        val moneyInNewCurrency = convertCurrency(from = to.amount, to = accordingTo.currency)

        to.status = InvoiceStatus.CANCELED
        invoiceService.update(to)

        return createInvoice(ofAmount = moneyInNewCurrency, to = accordingTo)
    }

    private fun createNewInvoice(subscription: Subscription) : Invoice {
        val plan: Plan = this.planService.fetch(subscription.planId)
        val to: Customer = this.customerService.fetch(subscription.customerId)
        val invoice = createInvoice(plan.amount, to)
        subscription.latestInvoiceId = invoice.id
        subscriptionService.update(subscription)
        return invoice
    }

    private fun createInvoice(ofAmount: Money, to: Customer) : Invoice {
        return invoiceService.create(ofAmount, to)
    }

    private fun convertCurrency(from: Money, to: Currency) : Money {
        return currencyProvider.convert(from, to)
    }
}
