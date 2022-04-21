package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SubscriptionDal(db: Database) : AntaeusDal<Subscription>(db) {
    override fun create(entity: Subscription): Subscription? {
        val id = transaction(db) {
            // Insert the subscription and return its new id.
            SubscriptionTable.insert {
                it[this.customerId] = entity.customerId
                it[this.planId] = entity.planId
                it[this.cancelAtPeriodEnds] = entity.cancelAtPeriodEnds
                it[this.subscriptionStatus] = entity.subscriptionStatus.toString()
                it[this.currentPeriodStarts] = entity.currentPeriodStarts.toString()
                it[this.currentPeriodEnds] = entity.currentPeriodEnds.toString()
                it[this.created] = entity.created.toString()
                it[this.pendingInvoiceInterval] = entity.pendingInvoiceInterval.toString()
                it[this.latestInvoiceId] = entity.latestInvoiceId
            } get SubscriptionTable.id
        }

        return fetch(id)
    }

    override fun update(entity: Subscription): Int {
        return transaction(db) {
            // Update the subscription and return the number of rows updated.
            SubscriptionTable.update({ SubscriptionTable.id.eq(entity.id) }) {
                it[this.planId] = entity.planId
                it[this.subscriptionStatus] = entity.subscriptionStatus.toString()
                it[this.cancelAtPeriodEnds] = entity.cancelAtPeriodEnds
                it[this.currentPeriodStarts] = entity.currentPeriodStarts.toString()
                it[this.currentPeriodEnds] = entity.currentPeriodEnds.toString()
                it[this.pendingInvoiceInterval] = entity.pendingInvoiceInterval.toString()
                it[this.latestInvoiceId] = entity.latestInvoiceId
            }
        }
    }

    override fun fetch(id: Int): Subscription? {
        return transaction(db) {
            SubscriptionTable
                    .select { SubscriptionTable.id.eq(id) }
                    .firstOrNull()
                    ?.toSubscription()
        }
    }

    override fun fetchBy(field: String): Subscription? {
        TODO("Not yet implemented")
    }

    override fun fetchAll(): List<Subscription> {
        return transaction(db) {
            SubscriptionTable
                    .selectAll()
                    .map { it.toSubscription() }
        }
    }
}