/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.DBConnectionException
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class CustomerService(private val dal: CustomerDal) {

    private val logger = KotlinLogging.logger {}
    fun fetchAll(): List<Customer> {
        return dal.fetchAll()
    }

    fun fetch(id: Int): Customer {
        return dal.fetch(id) ?: throw CustomerNotFoundException(id)
    }

    fun create(customer: Customer): Customer {
        return dal.create(customer)
                ?: run {
                    logger.error { "Failed to create new customer." }
                    throw DBConnectionException()
                }
    }

    fun getInvoices(of: Customer): List<Invoice> {
        return dal.getInvoices(of)
    }
}
