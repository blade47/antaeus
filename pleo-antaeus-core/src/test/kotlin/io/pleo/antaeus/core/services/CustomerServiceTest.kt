package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import kotlin.random.Random

@ExtendWith(EmbeddedDb::class)
class CustomerServiceTest(db: Database) {

    private val customerService = CustomerService(dal = CustomerDal(db = db))
    private val invoiceService = InvoiceService(dal = InvoiceDal(db = db))

    private fun setupInitialData() {
        val customers = (1..100).mapNotNull {
            customerService.create( Customer(
                    currency = Currency.values()[Random.nextInt(0, Currency.values().size)])
            )
        }

        customers.forEach { customer ->
            (1..10).forEach { _ ->
                invoiceService.create(
                        amount = Money(
                                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                                currency = customer.currency
                        ),
                        to = customer)
            }
        }
    }

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }

    @Test
    fun `retrieve customers`() {
        setupInitialData()
        val customers = customerService.fetchAll()
        assert(100 == customers.count())
    }

    @Test
    fun `check if unit test extension is emptying the database`() {
        val customers = customerService.fetchAll()
        assert(customers.isEmpty())
    }

    @Test
    fun `retrieve customer`() {
        setupInitialData()
        val customer = customerService.fetch(1)
        assert(customer.id == 1)
    }

    @Test
    fun `retrieve invoices for a given customer`() {
        setupInitialData()
        val customer = customerService.fetch(1)
        assert(customer.id == 1)
        val invoices = customerService.getInvoices(customer)
        assert(invoices.isNotEmpty())
        assert(invoices.count() == 10)
    }
}
