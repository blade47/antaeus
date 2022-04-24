package io.pleo.antaeus.app
import io.pleo.antaeus.core.exceptions.InvalidCurrencyException
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(invoiceDal: InvoiceDal, planDal: PlanDal, customerDal: CustomerDal) {
    val customers = (1..100).mapNotNull {
        customerDal.create( Customer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)])
        )
    }

    planDal.create( Plan(
            description = PlanDescription.STANDARD,
            amount = Money(
                    value = BigDecimal(15.0),
                    currency = Currency.USD))
    )
    planDal.create( Plan(
            description = PlanDescription.PREMIUM,
            amount = Money(
                    value = BigDecimal(450.0),
                    currency = Currency.USD))
    )

    customers.forEach { customer ->
        (1..10).forEach {
            invoiceDal.create( Invoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customerId = customer.id,
                status = (if (it == 1) invoiceDal.getStatus(InvoiceStatuses.PENDING) else invoiceDal.getStatus(InvoiceStatuses.PAID))!!))
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            return Random.nextBoolean()
        }
    }
}

// This is the mocked instance of the currency provider
internal fun getCurrencyProvider(): CurrencyProvider {
    return object : CurrencyProvider {
        override val currencyRates: Map<Currency, Double>
            get() = mapOf(
                    Currency.USD to 1.0,
                    Currency.DKK to 6.8120,
                    Currency.EUR to 0.91556,
                    Currency.GBP to 0.76514,
                    Currency.SEK to 9.3869
            )
        override fun convert(from: Money, to: Currency): Money {
            if (from.currency == to) {
                return from
            }
            if ( ! currencyRates.containsKey(from.currency) || ! currencyRates.containsKey(to)) {
                throw InvalidCurrencyException()
            }

            return Money(BigDecimal((from.value.toDouble() / currencyRates[from.currency]!!) * currencyRates[to]!!), to)
        }
    }
}
