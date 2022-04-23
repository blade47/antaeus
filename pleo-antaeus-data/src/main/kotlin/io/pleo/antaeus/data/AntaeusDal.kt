package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import org.jetbrains.exposed.sql.Database
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
                    .slice(InvoiceTable.id, InvoiceTable.currency, InvoiceTable.value, InvoiceTable.customerId, InvoiceTable.status)
                    .select { CustomerTable.id.eq(of.id) }
                    .map { it.toInvoice() }
        }
    }
}
