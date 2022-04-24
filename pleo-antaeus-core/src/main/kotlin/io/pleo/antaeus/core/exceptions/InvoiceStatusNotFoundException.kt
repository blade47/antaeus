package io.pleo.antaeus.core.exceptions

class InvoiceStatusNotFoundException(status: String) : StatusNotFoundException("InvoiceStatus", status)