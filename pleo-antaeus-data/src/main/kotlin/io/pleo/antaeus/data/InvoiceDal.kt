package io.pleo.antaeus.data

import io.pleo.antaeus.models.Invoice
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class InvoiceDal(db: Database) : AntaeusDal<Invoice>(db) {

    override fun create(entity: Invoice): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = entity.amount.value
                        it[this.currency] = entity.amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = entity.customerId
                    } get InvoiceTable.id
        }

        return this.fetch(id)
    }

    override fun update(entity: Invoice): Int {
        val numRowsUpdated = transaction(db) {
            // Update the invoice and returns the number of rows updated.
            InvoiceTable
                    .update ({ InvoiceTable.id.eq(entity.id) }) {
                        it[this.value] = entity.amount.value
                        it[this.currency] = entity.amount.currency.toString()
                        it[this.status] = status.toString()
                    }
        }

        return numRowsUpdated
    }

    override fun fetch(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    override fun fetchBy(field: String): Invoice? {
        TODO("Not yet implemented")
    }

    override fun fetchAll(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }
}