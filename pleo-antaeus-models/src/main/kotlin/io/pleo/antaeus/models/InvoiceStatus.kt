package io.pleo.antaeus.models

enum class InvoiceStatuses {
    PENDING,
    PAID,
    CANCELED
}

data class InvoiceStatus(
    val id: Int = Int.MIN_VALUE,
    var status: InvoiceStatuses,
    val description: String
)