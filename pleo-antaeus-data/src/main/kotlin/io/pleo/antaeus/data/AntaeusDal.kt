package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Database

abstract class AntaeusDal<T>(protected val db: Database) {
    abstract fun create(entity: T): T?
    abstract fun update(entity: T): Int
    abstract fun fetch(id: Int): T?
    abstract fun fetchBy(field: String): T?
    abstract fun fetchAll(): List<T>

    fun getCustomerInvoices() {
        // TODO: 21/04/22

    }
}