/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.CurrentDateTime
import java.time.LocalDate

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status") 
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
}

object SubscriptionTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val customerId = reference("customer_id", CustomerTable.id)
    val planId = reference("plan_id", PlanTable.id)
    val subscriptionStatus = text("subscription_status")
    val cancelAtPeriodEnds = bool("cancel_at_period_ends")
    val currentPeriodStarts = varchar("current_period_starts", 50)
    val currentPeriodEnds = varchar("current_period_ends", 50)
    val created = varchar("created", 50)
    val canceledAt = varchar("canceled_at", 50).nullable()
    val pendingInvoiceInterval = text("pending_invoice_interval")
    val latestInvoiceId = reference("invoice_id", InvoiceTable.id).nullable()
}
