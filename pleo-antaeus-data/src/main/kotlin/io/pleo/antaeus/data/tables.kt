/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table

object InvoiceStatusTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val status = text("status")
    val description = text("description")
}

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val statusId = reference("status_id", InvoiceStatusTable.id)
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}

object PlanTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val description = text("description").uniqueIndex()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val invoiceInterval = text("invoice_interval")
}

object SubscriptionStatusTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val status = text("status")
    val description = text("description")
}

object SubscriptionTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val customerId = reference("customer_id", CustomerTable.id)
    val planId = reference("plan_id", PlanTable.id)
    val statusId = reference("status_id", SubscriptionStatusTable.id)
    val cancelAtPeriodEnds = bool("cancel_at_period_ends")
    val currentPeriodStarts = varchar("current_period_starts", 50)
    val currentPeriodEnds = varchar("current_period_ends", 50)
    val created = varchar("created", 50)
    val canceledAt = varchar("canceled_at", 50).nullable()
    val latestInvoiceId = reference("invoice_id", InvoiceTable.id).nullable()
}
