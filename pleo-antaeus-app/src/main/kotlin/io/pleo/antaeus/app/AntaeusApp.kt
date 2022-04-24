/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.*
import io.pleo.antaeus.data.*
import io.pleo.antaeus.rest.AntaeusRest
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

private val logger = KotlinLogging.logger {}

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, PlanTable, SubscriptionTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = "")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val invoiceDal = InvoiceDal(db = db)
    val planDal = PlanDal(db = db)
    val subscriptionDal = SubscriptionDal(db = db)
    val customerDal = CustomerDal(db = db)

    // Insert example data in the database.
    setupInitialData(invoiceDal, planDal, customerDal)

    // Get third parties
    val paymentProvider = getPaymentProvider()
    val currencyProvider = getCurrencyProvider()
    val notificationProvider = getNotificationProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = invoiceDal)
    val customerService = CustomerService(dal = customerDal)
    val subscriptionService = SubscriptionService(dal = subscriptionDal)
    val planService = PlanService(dal = planDal)

    try {
        // Run as a background task to update daily the subscription table and charge users
        val billingService = BillingService(
                paymentProvider = paymentProvider,
                currencyProvider = currencyProvider,
                notificationProvider = notificationProvider,
                customerService = customerService,
                invoiceService = invoiceService,
                subscriptionService = subscriptionService,
                planService = planService
        )
    } catch(e: Exception) {
        logger.error(e) { "Error during the execution of the billing service: $e "}
    }

    // Create REST web service
    AntaeusRest(
            invoiceService = invoiceService,
            customerService = customerService
    ).run()
}
