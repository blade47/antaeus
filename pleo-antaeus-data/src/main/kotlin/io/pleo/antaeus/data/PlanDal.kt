package io.pleo.antaeus.data

import io.pleo.antaeus.models.Plan
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class PlanDal(db: Database) : AntaeusDal<Plan>(db) {

    override fun create(entity: Plan): Plan? {
        val id = transaction(db) {
            // Insert the plan and return its new id.
            PlanTable.insert {
                it[this.description] = entity.description.toString()
                it[this.value] = entity.amount.value
                it[this.currency] = entity.amount.currency.toString()
            } get PlanTable.id
        }
        return this.fetch(id)
    }

    override fun update(entity: Plan): Int {
        TODO("Not yet implemented")
    }

    override fun fetch(id: Int): Plan? {
        return transaction(db) {
            PlanTable
                    .select { PlanTable.id.eq(id) }
                    .firstOrNull()
                    ?.toPlan()
        }
    }

    override fun fetchBy(field: String): Plan? {
        return transaction(db) {
            PlanTable
                    .select { PlanTable.description.eq(field) }
                    .firstOrNull()
                    ?.toPlan()
        }
    }

    override fun fetchAll(): List<Plan> {
        return transaction(db) {
            PlanTable
                    .selectAll()
                    .map { it.toPlan() }
        }
    }
}