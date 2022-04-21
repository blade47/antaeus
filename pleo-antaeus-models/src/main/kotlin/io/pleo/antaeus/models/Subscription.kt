package io.pleo.antaeus.models

import java.time.LocalDate

data class Subscription(
        val id: Int = Int.MIN_VALUE,
        val customerId: Int,
        val planId: Int,
        val subscriptionStatus: SubscriptionStatus,
        val cancelAtPeriodEnds: Boolean,
        val currentPeriodStarts: LocalDate,
        val currentPeriodEnds: LocalDate,
        val created: LocalDate = LocalDate.now(),
        val canceledAt: LocalDate? = null,
        val pendingInvoiceInterval: InvoiceInterval,
        var latestInvoiceId: Int
)