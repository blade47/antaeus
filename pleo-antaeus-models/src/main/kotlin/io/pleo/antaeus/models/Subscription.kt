package io.pleo.antaeus.models

import java.time.LocalDate

data class Subscription(
    val id: Int = Int.MIN_VALUE,
    val customerId: Int,
    val plan: Plan,
    var status: SubscriptionStatus,
    val cancelAtPeriodEnds: Boolean,
    var currentPeriodStarts: LocalDate,
    var currentPeriodEnds: LocalDate,
    val created: LocalDate = LocalDate.now(),
    var canceledAt: LocalDate? = null,
    var latestInvoiceId: Int?
)