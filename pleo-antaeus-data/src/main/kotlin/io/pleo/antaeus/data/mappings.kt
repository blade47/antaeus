/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate

fun ResultRow.toInvoiceStatus(): InvoiceStatus = InvoiceStatus(
    id = this[InvoiceStatusTable.id],
    status = InvoiceStatuses.valueOf(this[InvoiceStatusTable.status]),
    description = this[InvoiceStatusTable.description],
)

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus(
        id = this[InvoiceStatusTable.id],
        status = InvoiceStatuses.valueOf(this[InvoiceStatusTable.status]),
        description = this[InvoiceStatusTable.description]),
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
    ),
    invoiceInterval = InvoiceInterval.valueOf(this[PlanTable.invoiceInterval])
)

fun ResultRow.toSubscriptionStatus(): SubscriptionStatus = SubscriptionStatus(
    id = this[SubscriptionStatusTable.id],
    status = SubscriptionStatuses.valueOf(this[SubscriptionStatusTable.status]),
    description = this[SubscriptionStatusTable.description],
)

fun ResultRow.toSubscription(): Subscription = Subscription(
    id = this[SubscriptionTable.id],
    customerId = this[SubscriptionTable.customerId],
    plan = Plan(
        id = this[PlanTable.id],
        description = PlanDescription.valueOf(this[PlanTable.description]),
        amount = Money(
            value = this[PlanTable.value],
            currency = Currency.valueOf(this[PlanTable.currency])
        ),
        invoiceInterval = InvoiceInterval.valueOf(this[PlanTable.invoiceInterval])),
    status = SubscriptionStatus(
        id = this[SubscriptionStatusTable.id],
        status = SubscriptionStatuses.valueOf(this[SubscriptionStatusTable.status]),
        description = this[SubscriptionStatusTable.description]),
    cancelAtPeriodEnds = this[SubscriptionTable.cancelAtPeriodEnds],
    currentPeriodStarts = LocalDate.parse(this[SubscriptionTable.currentPeriodStarts]),
    currentPeriodEnds = LocalDate.parse(this[SubscriptionTable.currentPeriodEnds]),
    created = LocalDate.parse(this[SubscriptionTable.created]),
    canceledAt = this[SubscriptionTable.canceledAt]?.let { s -> LocalDate.parse(s) },
    latestInvoiceId = this[SubscriptionTable.latestInvoiceId]
)
