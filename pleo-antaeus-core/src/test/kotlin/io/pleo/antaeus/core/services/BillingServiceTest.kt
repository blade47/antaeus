package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.data.PlanDal
import io.pleo.antaeus.data.SubscriptionDal
import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import kotlin.random.Random

@ExtendWith(EmbeddedDb::class)
@ExtendWith(MockKExtension::class)
class BillingServiceTest(db: Database){

    @RelaxedMockK
    private lateinit var paymentProvider: PaymentProvider

    @RelaxedMockK
    private lateinit var currencyProvider: CurrencyProvider

    private fun createRandomCustomer() : Customer {
        return customerService.create( Customer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)])
        )
    }

    private fun createRandomPlan() : Plan {
        return planService.create( Plan(
            description = PlanDescription.STANDARD,
            amount = Money(
                value = BigDecimal(15.0),
                currency = Currency.USD))
        )
    }

    private val planService = PlanService(dal = PlanDal(db = db))
    private val subscriptionService = SubscriptionService(dal = SubscriptionDal(db = db))
    private val customerService = CustomerService(dal = CustomerDal(db = db))
    private val invoiceService = InvoiceService(dal = InvoiceDal(db = db))

//    private val billingService = BillingService(
//        paymentProvider, currencyProvider, customerService, invoiceService, subscriptionService, planService
//    )

    @Test
    fun `should have a valid connection`() {
        val customer = createRandomCustomer()
        createRandomPlan()

//        every { paymentProvider.charge(any()) } returns true

        val billingService = BillingService(
            paymentProvider, currencyProvider, customerService, invoiceService, subscriptionService, planService
        )

        val subscription = billingService.createSubscription(customer)
        Assertions.assertNotNull(subscription)
//        every { paymentProvider.charge(any()) } returns true

//        println(paymentProvider.charge(mockk(relaxed = true)))
//        Assertions.assertNotNull(planService)
    }

    @Test
    fun `invoice a new subscription without pending invoices`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())
        every { paymentProvider.charge(any()) } returns true

        val billingService = BillingService(
            paymentProvider, currencyProvider, customerService, invoiceService, subscriptionService, planService
        )

        val customer = createRandomCustomer()
        val plan = createRandomPlan()

        val subscription = billingService.createSubscription(customer)

        Assertions.assertNotNull(subscription)
        Assertions.assertTrue(billingService.invoiceSubscription(subscription))
        Assertions.assertNotNull(subscription.latestInvoiceId?.let { invoiceService.fetch(it) })
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).id } == 1)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).status } == InvoiceStatus.PAID)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).amount.currency } == plan.amount.currency)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).amount.value } == plan.amount.value)
    }
}
