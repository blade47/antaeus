package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatuses
import io.pleo.antaeus.models.SubscriptionStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

abstract class AntaeusDal<T>(protected val db: Database) {
    abstract fun create(entity: T): T?
    abstract fun update(entity: T): Int
    abstract fun fetch(id: Int): T?
    abstract fun fetchAll(): List<T>

    fun getInvoices(of: Customer): List<Invoice> {
        return transaction(db) {
            CustomerTable
                .innerJoin(InvoiceTable)
                .innerJoin(InvoiceStatusTable)
                .select { CustomerTable.id.eq(of.id) }
                .map { it.toInvoice() }
        }
    }

    fun getPendingInvoice(of: Customer): Invoice? {
        return transaction(db) {
            CustomerTable
                .innerJoin(InvoiceTable)
                .innerJoin(InvoiceStatusTable)
                .select { CustomerTable.id.eq(of.id) and InvoiceStatusTable.status.eq(InvoiceStatuses.PENDING.toString()) }
                .map { it.toInvoice() }
        }.firstOrNull()
    }
}
