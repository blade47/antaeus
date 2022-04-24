package io.pleo.antaeus.core.exceptions

class SubscriptionStatusNotFoundException(status: String) : StatusNotFoundException("SubscriptionStatus", status)