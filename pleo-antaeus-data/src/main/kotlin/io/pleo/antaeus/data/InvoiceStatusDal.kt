package io.pleo.antaeus.data

import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatuses
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class InvoiceStatusDal(db: Database) : AntaeusDal<InvoiceStatus>(db) {
    override fun create(entity: InvoiceStatus): InvoiceStatus? {
        val id = transaction(db) {
            // Insert the invoice status and returns its new id.
            InvoiceStatusTable
                .insert {
                    it[this.status] = entity.status.toString()
                    it[this.description] = entity.description
                } get InvoiceStatusTable.id
        }

        return this.fetch(id)
    }

    override fun update(entity: InvoiceStatus): Int {
        TODO("Not yet implemented")
    }

    override fun fetch(id: Int): InvoiceStatus? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice status with matching id.
            InvoiceStatusTable
                .select { InvoiceStatusTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoiceStatus()
        }
    }

    override fun fetchAll(): List<InvoiceStatus> {
        return transaction(db) {
            InvoiceStatusTable
                .selectAll()
                .map { it.toInvoiceStatus() }
        }
    }

    fun fetchByStatus(status: String): InvoiceStatus? {
        return transaction(db) {
            InvoiceStatusTable
                .select { InvoiceStatusTable.status.eq(status) }
                .firstOrNull()
                ?.toInvoiceStatus()
        }
    }
}