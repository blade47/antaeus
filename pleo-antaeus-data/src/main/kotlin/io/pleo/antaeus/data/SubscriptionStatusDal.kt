package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatuses
import io.pleo.antaeus.models.SubscriptionStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SubscriptionStatusDal(db: Database) : AntaeusDal<SubscriptionStatus>(db) {
    override fun create(entity: SubscriptionStatus): SubscriptionStatus? {
        val id = transaction(db) {
            // Insert the invoice status and returns its new id.
            SubscriptionStatusTable
                .insert {
                    it[this.status] = entity.status.toString()
                    it[this.description] = entity.description
                } get SubscriptionStatusTable.id
        }

        return this.fetch(id)
    }

    override fun update(entity: SubscriptionStatus): Int {
        TODO("Not yet implemented")
    }

    override fun fetch(id: Int): SubscriptionStatus? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first subscription status with matching id.
            SubscriptionStatusTable
                .select { SubscriptionStatusTable.id.eq(id) }
                .firstOrNull()
                ?.toSubscriptionStatus()
        }
    }

    override fun fetchAll(): List<SubscriptionStatus> {
        return transaction(db) {
            SubscriptionStatusTable
                .selectAll()
                .map { it.toSubscriptionStatus() }
        }
    }

    fun fetchByStatus(status: String): SubscriptionStatus? {
        return transaction(db) {
            SubscriptionStatusTable
                .select { SubscriptionStatusTable.status.eq(status) }
                .firstOrNull()
                ?.toSubscriptionStatus()
        }
    }
}