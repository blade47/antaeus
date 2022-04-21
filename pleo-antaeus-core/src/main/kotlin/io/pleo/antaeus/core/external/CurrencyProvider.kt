/*
    This is the payment provider. It is a "mock" of an external service that you can pretend runs on another system.
    With this API you can ask customers to pay an invoice.

    This mock will succeed if the customer has enough money in their balance,
    however the documentation lays out scenarios in which paying an invoice could fail.
 */

package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money

interface CurrencyProvider {

    val currencyRates: Map<Currency, Double>

    /*
        Convert from one currency to another.

        Returns:
          `Money`object containing the new converted currency.

        Throws:
          `InvalidCurrencyException`: when one or both provided currencies are invalid.
     */

    fun convert(from: Money, to: Currency): Money
}