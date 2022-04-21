package io.pleo.antaeus.models

enum class InvoiceInterval(val days: Long) {
    DAY(1),
    WEEK(7),
    MONTH(30),
    YEAR(365)
}