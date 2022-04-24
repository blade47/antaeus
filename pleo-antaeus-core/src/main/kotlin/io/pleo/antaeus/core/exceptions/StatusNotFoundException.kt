package io.pleo.antaeus.core.exceptions

abstract class StatusNotFoundException(entity: String, status: String) : Exception("$entity '$status' was not found")