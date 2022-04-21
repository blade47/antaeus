package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class CustomerDal(db: Database) : AntaeusDal<Customer>(db) {

    override fun create(entity: Customer): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = entity.currency.toString()
            } get CustomerTable.id
        }

        return this.fetch(id)
    }

    override fun update(entity: Customer): Int {
        TODO("Not yet implemented")
    }

    override fun fetch(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                    .select { CustomerTable.id.eq(id) }
                    .firstOrNull()
                    ?.toCustomer()
        }
    }

    override fun fetchBy(field: String): Customer? {
        TODO("Not yet implemented")
    }

    override fun fetchAll(): List<Customer> {
        return transaction(db) {
            CustomerTable
                    .selectAll()
                    .map { it.toCustomer() }
        }
    }
}