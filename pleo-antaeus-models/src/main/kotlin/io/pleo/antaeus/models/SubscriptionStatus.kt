package io.pleo.antaeus.models

enum class SubscriptionStatuses {
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    ACTIVE,
    PAST_DUE,
    CANCELED
}

data class SubscriptionStatus(
    val id: Int = Int.MIN_VALUE,
    var status: SubscriptionStatuses,
    val description: String
)