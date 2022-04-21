/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)

fun ResultRow.toPlan(): Plan = Plan(
    id = this[PlanTable.id],
    description = PlanDescription.valueOf(this[PlanTable.description]),
    amount = Money(
        value = this[PlanTable.value],
        currency = Currency.valueOf(this[PlanTable.currency])
    )
)

fun ResultRow.toSubscription(): Subscription = Subscription(
    id = this[SubscriptionTable.id],
    customerId = this[SubscriptionTable.customerId],
    planId = this[SubscriptionTable.planId],
    subscriptionStatus = SubscriptionStatus.valueOf(this[SubscriptionTable.subscriptionStatus]),
    cancelAtPeriodEnds = this[SubscriptionTable.cancelAtPeriodEnds],
    currentPeriodStarts = LocalDate.parse(this[SubscriptionTable.currentPeriodStarts]),
    currentPeriodEnds = LocalDate.parse(this[SubscriptionTable.currentPeriodEnds]),
    created = LocalDate.parse(this[SubscriptionTable.created]),
    canceledAt = LocalDate.parse(this[SubscriptionTable.canceledAt]),
    pendingInvoiceInterval = InvoiceInterval.valueOf(this[SubscriptionTable.pendingInvoiceInterval]),
    latestInvoiceId = this[SubscriptionTable.latestInvoiceId]
)
