package io.pleo.antaeus.models

data class Invoice(
        val id: Int = Int.MIN_VALUE,
        val customerId: Int,
        val amount: Money,
        var status: InvoiceStatus
)
