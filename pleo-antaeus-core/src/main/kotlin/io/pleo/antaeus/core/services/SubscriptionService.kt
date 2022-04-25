/*
    Implements endpoints related to subscriptions.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.DBConnectionException
import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.core.exceptions.SubscriptionStatusNotFoundException
import io.pleo.antaeus.data.SubscriptionDal
import io.pleo.antaeus.models.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionService(private val dal: SubscriptionDal) {

    private val invoiceIntervalDefault = InvoiceInterval.MONTH
    private val logger = KotlinLogging.logger {}

    fun fetchAll(): List<Subscription> {
        return dal.fetchAll()
    }

    fun fetch(id: Int): Subscription {
        return dal.fetch(id) ?: throw SubscriptionNotFoundException(id)
    }

    fun subscribe(customer: Customer, to: Plan): Subscription {
        val latestInvoice: Invoice? = this.getPendingInvoice(customer)
        return dal.create( Subscription(
                customerId = customer.id,
                planId = to.id,
                status = dal.getStatus(SubscriptionStatuses.INCOMPLETE) ?: run { throw SubscriptionStatusNotFoundException(SubscriptionStatuses.INCOMPLETE.toString()) },
                cancelAtPeriodEnds = false,
                currentPeriodStarts = LocalDate.now(),
                currentPeriodEnds = LocalDate.now().plusDays(this.invoiceIntervalDefault.days),
                pendingInvoiceInterval = this.invoiceIntervalDefault,
                latestInvoiceId = latestInvoice?.id)
        ) ?: run {
            logger.error { "Failed to subscribe customer ${customer.id} to ${to.id}" }
            throw DBConnectionException()
        }
    }

    fun renew(subscription: Subscription): Boolean {
        logger.trace { "Renewing subscription ${subscription.id}" }
        subscription.currentPeriodStarts = subscription.currentPeriodEnds
        subscription.currentPeriodEnds = subscription.currentPeriodEnds.plusDays(this.invoiceIntervalDefault.days)
        if (subscription.status.status != SubscriptionStatuses.ACTIVE) subscription.status = this.getStatus(SubscriptionStatuses.ACTIVE)
        return this.update(subscription)
    }

    private fun update(subscription: Subscription): Boolean {
        if (dal.update(subscription) != 1){
            logger.error { "Failed to update subscription status for subscription ${subscription.id}" }
            throw DBConnectionException()
        }
        return true
    }

    fun cancel(subscription: Subscription) {
        logger.trace { "Cancelling subscription ${subscription.id}" }
        subscription.status = this.getStatus(SubscriptionStatuses.CANCELED)
        subscription.canceledAt = LocalDate.now()
        this.update(subscription)
    }

    fun activate(subscription: Subscription) {
        logger.trace { "Activating subscription ${subscription.id}" }
        subscription.status = this.getStatus(SubscriptionStatuses.ACTIVE)
        this.update(subscription)
    }

    fun pastDue(subscription: Subscription) {
        logger.trace { "Subscription ${subscription.id} past due." }
        subscription.status = this.getStatus(SubscriptionStatuses.PAST_DUE)
        this.update(subscription)
    }

    fun expire(subscription: Subscription) {
        logger.trace { "Subscription ${subscription.id} expired." }
        subscription.status = this.getStatus(SubscriptionStatuses.INCOMPLETE_EXPIRED)
        this.update(subscription)
    }

    fun updateLatestInvoice(to: Subscription, with: Invoice) {
        logger.trace { "Updating latest invoice for subscription: ${to.id} with invoice: ${with.id}." }
        to.latestInvoiceId = with.id
        this.update(to)
    }

    private fun getStatus(status: SubscriptionStatuses): SubscriptionStatus {
        return dal.getStatus(status) ?: throw SubscriptionStatusNotFoundException(status.toString())
    }

    fun createStatus(status: SubscriptionStatuses, description: String): SubscriptionStatus{
        return dal.createStatus(status, description) ?: throw DBConnectionException()
    }

    fun getPendingInvoice(of: Customer) : Invoice? {
        return dal.getPendingInvoice(of) ?: run { null }
    }

    private fun getInvoices(of: Customer) : List<Invoice> {
        return dal.getInvoices(of)
    }
}