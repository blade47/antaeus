package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.*
import java.sql.Connection

class EmbeddedDb : BeforeAllCallback, AfterEachCallback, ParameterResolver {

    private val tables = arrayOf(InvoiceTable, CustomerTable, PlanTable, SubscriptionTable)
    private lateinit var db: Database

    override fun beforeAll(context: ExtensionContext) {
        this.db = Database
                .connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;MODE=MYSQL", "org.h2.Driver")
                .also {
                    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                    transaction {
                        SchemaUtils.create(*tables)
                    }
                }

//        if (context.root.getStore(ExtensionContext.Namespace.create(EmbeddedDb::class.java))["db", Database::class.java] == null) {
//            context.root.getStore(ExtensionContext.Namespace.create(EmbeddedDb::class.java)).put("db", db)
//        } else {
//            // this is never executed
//            println("Found it, no need to store anything again!")
//        }
    }
    override fun afterEach(context: ExtensionContext) {
        transaction {
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext?,
                                   extensionContext: ExtensionContext?) =
            parameterContext?.parameter?.type == Database::class.java

    override fun resolveParameter(parameterContext: ParameterContext?,
                                  extensionContext: ExtensionContext?) =
            db
}