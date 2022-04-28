package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.task.TimerTask
import io.pleo.antaeus.core.utils.RetryOnException
import io.pleo.antaeus.models.*
import mu.KotlinLogging
import java.time.LocalDate
import java.time.Period
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val currencyProvider: CurrencyProvider,
    private val notificationProvider: NotificationProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService,
    private val subscriptionService: SubscriptionService,
    private val planService: PlanService
) {
    private val logger = KotlinLogging.logger {}
    private val task = TimerTask.create("billingRoutineTask", repeat = 12.hours) {
        this.billingRoutine()
    }
    fun setupInitialData() {
        val customers = this.customerService.fetchAll()
        customers.forEach { customer ->
            try {
                // For already existent customers we pick a random plan to apply
                val randomPlanDescription = PlanDescription.values()[Random.nextInt(0, PlanDescription.values().size)]
                val plan: Plan = try { this.planService.getByDescription(randomPlanDescription) } catch (e: PlanNotFoundException) { planService.fetchFirst() }

                val subscription: Subscription = this.createSubscription(to = customer, with = plan)
                if ( this.invoiceSubscription(subscription) ) this.subscriptionService.activate(subscription)

            } catch (e: Exception) {
                logger.error(e) { "Failed to handle subscription for customer ${customer.id}. $e" }
                notificationProvider.send(Notification(1, "Unexpected exception while handling subscription of ${customer.id}"))
            }
        }
    }

    // To run twice a day so if we have network problems we cover it
    fun billingRoutine(currentDate: LocalDate? = null) {
        logger.trace { "Billing routine started..." }
        val subscriptions = this.subscriptionService.fetchAll()
        val today = currentDate ?: LocalDate.now()
        subscriptions.forEach { subscription ->
            when (subscription.status.status) {
                SubscriptionStatuses.ACTIVE -> {
                    logger.trace { "Active subscription processing..." }
                    if (subscription.currentPeriodEnds.isEqual(today) || subscription.currentPeriodEnds.isBefore(today)) {
                        if ( ! subscription.cancelAtPeriodEnds)
                            if ( this.invoiceSubscription(subscription) ) {
                                subscriptionService.renew(subscription)
                                logger.info { "Subscription ${subscription.id} renewed." }
                                notificationProvider.send(
                                    Notification(
                                        subscription.customerId,
                                        "The subscription is now renewed, thank you."
                                    )
                                )
                            }
                            else {
                                this.subscriptionService.pastDue(subscription)
                                logger.info { "Subscription ${subscription.id} past due." }
                                notificationProvider.send(
                                    Notification(
                                        subscription.customerId,
                                        "Your subscription was renewed but the payment failed, please ensure to have enough money for the subscription."
                                    )
                                )
                            }
                        else {
                            this.subscriptionService.cancel(subscription)
                            logger.info { "Subscription ${subscription.id} canceled." }
                            notificationProvider.send(
                                Notification(
                                    subscription.customerId,
                                    "As requested, your subscription is now canceled, thank you."
                                )
                            )
                        }
                    }
                }
                SubscriptionStatuses.INCOMPLETE -> {
                    logger.trace { "Incomplete subscription processing..." }
                    if (Period.between(subscription.created, today).days <= 3) {
                        if (this.invoiceSubscription(subscription)) {
                            this.subscriptionService.activate(subscription)
                            logger.info { "Subscription ${subscription.id} activated." }
                            notificationProvider.send(
                                Notification(
                                    subscription.customerId,
                                    "The subscription is now active, thank you."
                                )
                            )
                        }
                    }
                    else {
                        this.subscriptionService.expire(subscription)
                        logger.info { "Subscription ${subscription.id} expired." }
                        notificationProvider.send(
                            Notification(
                                subscription.customerId,
                                "We were not able to charge your first invoice for activating the subscription, we are therefore cancelling it."
                            )
                        )
                    }
                }
                SubscriptionStatuses.CANCELED -> {
                    // TODO: 24/04/22 For future developments
                }
                SubscriptionStatuses.INCOMPLETE_EXPIRED -> {
                    // TODO: 24/04/22 For future developments
                }
                SubscriptionStatuses.PAST_DUE -> {
                    logger.trace { "Past due subscription processing..." }
                    if (Period.between(subscription.currentPeriodEnds, today).days <= 3) {
                        if ( this.invoiceSubscription(subscription) ) {
                            subscriptionService.renew(subscription)
                            logger.info { "Subscription ${subscription.id} renewed." }
                            notificationProvider.send(
                                Notification(
                                    subscription.customerId,
                                    "The subscription is now renewed, thank you."
                                )
                            )
                        }
                    } else {
                        this.subscriptionService.cancel(subscription)
                        logger.info { "Subscription ${subscription.id} canceled." }
                        notificationProvider.send(
                            Notification(
                                subscription.customerId,
                                "We were not able to charge your last invoice for keeping the subscription, we are therefore cancelling it."
                            )
                        )
                    }
                }
            }
        }
    }

    fun startBillingTask() {
        task.start()
    }

    fun stopBillingTask() {
        task.shutdown()
    }

    fun forceStopBillingTask() {
        task.cancel()
    }

    fun isBillingTaskRunning(): Boolean {
        return this.task.isRunning()
    }

    fun createSubscription(to: Customer, with: Plan): Subscription {
        return this.subscriptionService.subscribe(customer = to, to = with)
    }

    fun invoiceSubscription(subscription: Subscription): Boolean {

        val retryHandler = RetryOnException()

        while (true) {
            try {
                return this.charge(subscription)
            } catch (e: CustomerNotFoundException) {
                logger.error { "Failed to charge subscription fee for subscription ${subscription.id}. Customer not found." }
                notificationProvider.send(Notification(1, "Customer not found during charging of subscription ${subscription.id}"))
                this.subscriptionService.cancel(subscription)
                return false
            } catch (e: NetworkException) {
                retryHandler.exceptionOccurred(e);
                logger.warn { "Network error while charging subscription fee for subscription ${subscription.id}. Retrying..." }
            } catch (e: InvalidCurrencyException) {
                logger.error(e) { "Failed to convert currency for subscription ${subscription.id}. $e" }
                notificationProvider.send(Notification(1, "Failed to convert currency of subscription ${subscription.id}"))
                this.subscriptionService.cancel(subscription)
                return false
            } catch (e: Exception) {
                logger.error(e) { "Failed to charge subscription fee for subscription ${subscription.id}. $e" }
                this.subscriptionService.cancel(subscription)
                notificationProvider.send(Notification(1, "Unexpected exception while charging subscription ${subscription.id}"))
                return false
            }
        }
    }

    private fun charge(subscription: Subscription) : Boolean {
        val customer: Customer = this.customerService.fetch(subscription.customerId)

        val invoiceToCharge: Invoice = try {
            subscription.latestInvoiceId?.let {
                val latestInvoice = this.invoiceService.fetch(it)
                if (latestInvoice.status.status != InvoiceStatuses.PENDING) {
                    val newInvoice = this.createNewInvoice(subscription)
                    this.subscriptionService.updateLatestInvoice(to = subscription, with = newInvoice)
                    newInvoice
                } else {
                    latestInvoice
                }
            }?: run {
                val newInvoice = this.createNewInvoice(subscription)
                this.subscriptionService.updateLatestInvoice(to = subscription, with = newInvoice)
                newInvoice
            }

        } catch (e: InvoiceNotFoundException) {
            logger.warn { "Failed to retrieve latest invoice for subscription ${subscription.id}." }
            val newInvoice = this.createNewInvoice(subscription)
            this.subscriptionService.updateLatestInvoice(to = subscription, with = newInvoice)
            newInvoice
        }

        try {
            if ( ! this.paymentProvider.charge(invoiceToCharge) ) {
                logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Account balance did not allow the charge." }
                return false;
            }
            logger.info { "Customer ${customer.id} charged successfully for subscription ${subscription.id}." }
            this.invoiceService.validate(invoiceToCharge)
            notificationProvider.send(Notification(
                customer.id,
                "Subscription for using our Antaeus APP has been charged successfully. Attached you can find the related invoice.",
                invoiceToCharge))
            return true;

        } catch (e: CurrencyMismatchException) {
            logger.warn { "Failed to charge subscription fee for customer ${customer.id} with subscription ${subscription.id}. Currency mismatch." }
            val newInvoice = adjustCurrency(to = invoiceToCharge, accordingTo = customer)
            this.subscriptionService.updateLatestInvoice(to = subscription, with = newInvoice)
            return charge(subscription = subscription)
        }
    }

    private fun adjustCurrency(to: Invoice, accordingTo: Customer) : Invoice {
        logger.info { "Creating new invoice with updated currency..." }
        val moneyInNewCurrency = convertCurrency(from = to.amount, to = accordingTo.currency)
        this.invoiceService.invalidate(to)
        return createInvoice(ofAmount = moneyInNewCurrency, to = accordingTo)
    }

    private fun createNewInvoice(subscription: Subscription) : Invoice {
        logger.info { "Creating new invoice for subscription ${subscription.id}." }
        val to: Customer = this.customerService.fetch(subscription.customerId)
        return if (subscription.plan.amount.currency == to.currency) createInvoice(ofAmount = subscription.plan.amount, to)
        else createInvoice( ofAmount = this.convertCurrency(subscription.plan.amount, to.currency), to)
    }

    private fun createInvoice(ofAmount: Money, to: Customer) : Invoice {
        return invoiceService.create(ofAmount, to)
    }

    private fun convertCurrency(from: Money, to: Currency) : Money {
        return currencyProvider.convert(from, to)
    }
}
