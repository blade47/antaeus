package io.pleo.antaeus.models

data class Plan(
    val id: Int = Int.MIN_VALUE,
    val description: PlanDescription,
    val amount: Money
)