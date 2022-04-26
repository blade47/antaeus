package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.random.Random

@ExtendWith(EmbeddedDb::class)
@ExtendWith(MockKExtension::class)
class BillingServiceTest(db: Database){

    private val planService = PlanService(dal = PlanDal(db = db))
    private val subscriptionService = SubscriptionService(dal = SubscriptionDal(db = db))
    private val customerService = CustomerService(dal = CustomerDal(db = db))
    private val invoiceService = InvoiceService(dal = InvoiceDal(db = db))
    private lateinit var billingService: BillingService

    @BeforeEach
    private fun setup() {
        createSubscriptionStatuses()
        createInvoiceStatuses()

        this.billingService = BillingService(
            paymentProvider, currencyProvider, notificationProvider, customerService, invoiceService, subscriptionService, planService
        )
    }

    @RelaxedMockK
    private lateinit var paymentProvider: PaymentProvider

    @RelaxedMockK
    private lateinit var currencyProvider: CurrencyProvider

    @RelaxedMockK
    private lateinit var notificationProvider: NotificationProvider

    private fun createSubscriptionStatuses() {
        SubscriptionStatuses.values().forEach { status -> subscriptionService.createStatus(status, status.toString()) }
    }

    private fun createInvoiceStatuses() {
        InvoiceStatuses.values().forEach { status -> invoiceService.createStatus(status, status.toString()) }
    }

    private fun createCustomer(currency: Currency? = null) : Customer {
        return customerService.create( Customer(
            currency = currency ?: run { Currency.values()[Random.nextInt(0, Currency.values().size)] })
        )
    }

    private fun createPlan(amount: Money? = null) : Plan {
        val randomPlanDescription = PlanDescription.values()[Random.nextInt(0, PlanDescription.values().size)]
        return planService.create( Plan(
            description = randomPlanDescription,
            amount = amount ?: run { Money(
                value = BigDecimal(15.0),
                currency = Currency.USD)
            },
            invoiceInterval = InvoiceInterval.MONTH)
        )
    }

    private fun setupInitialData() {

        createInvoiceStatuses()
        createSubscriptionStatuses()

        val customers = (1..100).mapNotNull {
            customerService.create( Customer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)])
            )
        }

        planService.create( Plan(
            description = PlanDescription.STANDARD,
            amount = Money(
                value = BigDecimal(15.0),
                currency = Currency.USD),
                invoiceInterval = InvoiceInterval.MONTH)
        )
        planService.create( Plan(
            description = PlanDescription.PREMIUM,
            amount = Money(
                value = BigDecimal(450.0),
                currency = Currency.USD),
            invoiceInterval = InvoiceInterval.MONTH)
        )

        customers.forEach { customer ->
            (1..10).forEach {
                invoiceService.create(
                    amount = Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = customer.currency
                    ),
                    to = customer,
                    withStatus = (if (it == 1) invoiceService.getStatus(InvoiceStatuses.PENDING) else invoiceService.getStatus(
                        InvoiceStatuses.PAID
                    ))
                )
            }
        }
    }

    @Test
    fun `create new subscription`() {
        val customer = createCustomer()
        val plan = createPlan()
        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)
        Assertions.assertTrue(subscription.status.status == SubscriptionStatuses.INCOMPLETE)
    }

    @Test
    fun `invoice subscription for customer with no pending invoices`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        every { paymentProvider.charge(any()) } returns true

        val customer = createCustomer()
        val plan = createPlan(Money(BigDecimal(18.0), customer.currency))

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)
        Assertions.assertNull(subscription.latestInvoiceId)

        val invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)
        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.id } == 1)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.currency } == customer.currency)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.value } == plan.amount.value)
        }
    }

    @Test
    fun `invoice subscription for customer with pending invoice`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())
        every { paymentProvider.charge(any()) } returns true

        val customer = createCustomer()
        val invoice = invoiceService.create(amount = Money(BigDecimal(10.0), currency = customer.currency), to = customer)
        val plan = createPlan()

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)
        Assertions.assertNotNull(subscription.latestInvoiceId)
        Assertions.assertTrue(invoice.id == subscription.latestInvoiceId)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).status.status } == InvoiceStatuses.PENDING)

        val invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)
        Assertions.assertTrue(invoice.id == subscription.latestInvoiceId)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.id } == invoice.id)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.currency } == customer.currency)
        }
    }

    @Test
    fun `invoice subscription with plan in USD currency for customer with EUR currency and no pending invoices`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        val customer = createCustomer(Currency.EUR)
        val plan = createPlan(amount = Money(BigDecimal(20.0), Currency.USD))

        every { paymentProvider.charge(any()) } returns true
        every { currencyProvider.convert(any(), any()) } returns Money(BigDecimal(18.3112), Currency.EUR)

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)

        val invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.value.toDouble() } == 18.31)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.currency } == customer.currency)
        }
    }

    @Test
    fun `invoice subscription for customer having last pending invoice with wrong currency`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        val customer = createCustomer(Currency.EUR)
        var wrongInvoice = invoiceService.create(amount = Money(BigDecimal(10.0), currency = Currency.USD), to = customer)
        val plan = createPlan()

        every { paymentProvider.charge(any()) } throws(CurrencyMismatchException(wrongInvoice.id, customer.id)) andThen(true)
        every { currencyProvider.convert(wrongInvoice.amount, Currency.EUR) } returns Money(BigDecimal(9.1556), Currency.EUR)

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)
        Assertions.assertNotNull(subscription.latestInvoiceId)
        Assertions.assertTrue(wrongInvoice.id == subscription.latestInvoiceId)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { invoiceService.fetch(it).status.status } == InvoiceStatuses.PENDING)

        val invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)
        Assertions.assertTrue(wrongInvoice.id != subscription.latestInvoiceId)

        wrongInvoice = invoiceService.fetch(wrongInvoice.id)

        Assertions.assertNotNull(wrongInvoice)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { wrongInvoice.amount.currency } == Currency.USD)
        Assertions.assertTrue(subscription.latestInvoiceId?.let { wrongInvoice.status.status } == InvoiceStatuses.CANCELED)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.id } != wrongInvoice.id)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.value.toDouble() } == 9.16)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.amount.currency } == customer.currency)
        }
    }

    @Test
    fun `charging subscriptions has multiple network errors`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        val customer = createCustomer(Currency.USD)
        val plan = createPlan()

        // One network exception

        every { paymentProvider.charge(any()) } throws(NetworkException()) andThen(true)

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)

        var invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        val latestInvoiceIdFirstPayment = subscription.latestInvoiceId

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
        }

        // Three network exceptions

        every { paymentProvider.charge(any()) } throws(NetworkException()) andThenThrows(NetworkException()) andThenThrows(NetworkException()) andThen(true)

        Assertions.assertNotNull(subscription)

        invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        val latestInvoiceIdSecondPayment = subscription.latestInvoiceId

        Assertions.assertTrue(latestInvoiceIdFirstPayment != latestInvoiceIdSecondPayment)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
        }

        // Only network exceptions

        every { paymentProvider.charge(any()) } throws(NetworkException())

        Assertions.assertThrows(NetworkException::class.java) {
            billingService.invoiceSubscription(subscription)
        }

        Assertions.assertNotNull(subscription.latestInvoiceId)
        Assertions.assertTrue(latestInvoiceIdFirstPayment != subscription.latestInvoiceId)
        Assertions.assertTrue(latestInvoiceIdSecondPayment != subscription.latestInvoiceId)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PENDING)
        }
    }

    @Test
    fun `multiple payments for subscription`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        val customer = createCustomer(Currency.USD)
        val plan = createPlan()

        every { paymentProvider.charge(any()) } returns true

        val subscription = billingService.createSubscription(to = customer, with = plan)

        Assertions.assertNotNull(subscription)

        // First payment

        var invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        val firstPaymentInvoiceId = subscription.latestInvoiceId

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
        }

        // Second payment

        invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        val secondPaymentInvoiceId = subscription.latestInvoiceId

        Assertions.assertTrue(secondPaymentInvoiceId != firstPaymentInvoiceId)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
        }

        // Third payment

        invoiced = billingService.invoiceSubscription(subscription)

        Assertions.assertTrue(invoiced)
        Assertions.assertNotNull(subscription.latestInvoiceId)

        val thirdPaymentInvoiceId = subscription.latestInvoiceId

        Assertions.assertTrue(firstPaymentInvoiceId != thirdPaymentInvoiceId)
        Assertions.assertTrue(secondPaymentInvoiceId != thirdPaymentInvoiceId)

        subscription.latestInvoiceId?.let {
            val latestInvoice = invoiceService.fetch(it)
            Assertions.assertNotNull(latestInvoice)
            Assertions.assertTrue(subscription.latestInvoiceId?.let { latestInvoice.status.status } == InvoiceStatuses.PAID)
        }

        val invoices = customerService.getInvoices(customer)

        // End

        Assertions.assertTrue(invoices.size == 3)

        invoices.forEach { invoice -> Assertions.assertTrue(invoice.status.status == InvoiceStatuses.PAID) }
    }

    @Test
    fun `billing routine`() {
        Assertions.assertTrue(subscriptionService.fetchAll().isEmpty())

        every { paymentProvider.charge(any()) } returns true
        every { currencyProvider.convert(any(), any()) } returns Money(BigDecimal(450), Currency.USD)

        setupInitialData()

        var customers = customerService.fetchAll()
        var invoices = invoiceService.fetchAll()
        var subscriptions = subscriptionService.fetchAll()

        Assertions.assertTrue(customers.size == 100)
        Assertions.assertTrue(invoices.size == 1000)
        Assertions.assertTrue(subscriptions.isEmpty())

        customers.forEach { customer ->
            Assertions.assertNotNull(subscriptionService.getPendingInvoice(customer))
        }

        var pendingInvoices = 0
        invoices.forEach { invoice ->
            if (invoice.status.status == InvoiceStatuses.PENDING) pendingInvoices++
        }

        Assertions.assertTrue(pendingInvoices == 100)

        billingService.setupInitialData()

        customers = customerService.fetchAll()
        invoices = invoiceService.fetchAll()
        subscriptions = subscriptionService.fetchAll()

        customers.forEach { customer ->
            Assertions.assertNull(subscriptionService.getPendingInvoice(customer))
        }

        pendingInvoices = 0
        invoices.forEach { invoice ->
            if (invoice.status.status == InvoiceStatuses.PENDING) pendingInvoices++
        }

        Assertions.assertTrue(pendingInvoices == 0)
        Assertions.assertTrue(invoices.size == 1000)
        Assertions.assertTrue(subscriptions.size == 100)

        var activeSubscriptions = 0
        subscriptions.forEach { subscription ->
            if (subscription.status.status == SubscriptionStatuses.ACTIVE) activeSubscriptions++
        }

        Assertions.assertTrue(activeSubscriptions == 100)

        billingService.billingRoutine()

        customers = customerService.fetchAll()
        invoices = invoiceService.fetchAll()
        subscriptions = subscriptionService.fetchAll()

        customers.forEach { customer ->
            Assertions.assertNull(subscriptionService.getPendingInvoice(customer))
        }

        pendingInvoices = 0
        invoices.forEach { invoice ->
            if (invoice.status.status == InvoiceStatuses.PENDING) pendingInvoices++
        }

        Assertions.assertTrue(pendingInvoices == 0)
        Assertions.assertTrue(invoices.size == 1000)
        Assertions.assertTrue(subscriptions.size == 100)

        every { paymentProvider.charge(any()) } returns false andThen true

        billingService.billingRoutine(currentDate = LocalDate.now().plusDays(30))

        invoices = invoiceService.fetchAll()
        subscriptions = subscriptionService.fetchAll()

        pendingInvoices = 0
        invoices.forEach { invoice ->
            if (invoice.status.status == InvoiceStatuses.PENDING) pendingInvoices++
        }

        activeSubscriptions = 0
        var pastDueSubscriptions = 0
        subscriptions.forEach { subscription ->
            if (subscription.status.status == SubscriptionStatuses.ACTIVE) activeSubscriptions++
            if (subscription.status.status == SubscriptionStatuses.PAST_DUE) pastDueSubscriptions++
        }

        Assertions.assertTrue(pendingInvoices == 1)
        Assertions.assertTrue(invoices.size == 1100)
        Assertions.assertTrue(subscriptions.size == 100)
        Assertions.assertTrue(activeSubscriptions == 99)
        Assertions.assertTrue(pastDueSubscriptions == 1)

        billingService.billingRoutine(currentDate = LocalDate.now().plusDays(1))

        invoices = invoiceService.fetchAll()
        subscriptions = subscriptionService.fetchAll()

        pendingInvoices = 0
        invoices.forEach { invoice ->
            if (invoice.status.status == InvoiceStatuses.PENDING) pendingInvoices++
        }

        activeSubscriptions = 0
        pastDueSubscriptions = 0
        subscriptions.forEach { subscription ->
            if (subscription.status.status == SubscriptionStatuses.ACTIVE) activeSubscriptions++
            if (subscription.status.status == SubscriptionStatuses.PAST_DUE) pastDueSubscriptions++
        }

        Assertions.assertTrue(pendingInvoices == 0)
        Assertions.assertTrue(invoices.size == 1100)
        Assertions.assertTrue(subscriptions.size == 100)
        Assertions.assertTrue(activeSubscriptions == 100)
        Assertions.assertTrue(pastDueSubscriptions == 0)
    }
}
