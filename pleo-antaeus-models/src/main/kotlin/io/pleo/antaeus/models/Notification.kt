package io.pleo.antaeus.models

data class Notification (
    val userId: Int,
    val message: String,
    val attachments: Invoice? = null
)