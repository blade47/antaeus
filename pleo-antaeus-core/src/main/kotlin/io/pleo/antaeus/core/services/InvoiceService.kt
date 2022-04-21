/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.DBConnectionException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging

class InvoiceService(private val dal: InvoiceDal) {

    private val logger = KotlinLogging.logger {}

    fun create(amount: Money, to: Customer) : Invoice {
        return dal.create( Invoice(
                amount = amount,
                customerId = to.id,
                status = InvoiceStatus.PENDING)
        ) ?: run {
            logger.error { "Failed to create invoice to customer ${to.id}" }
            throw DBConnectionException()
        }
    }

    fun fetchAll(): List<Invoice> {
        return dal.fetchAll()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetch(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(invoice: Invoice) : Boolean {
        if ( dal.update(invoice) != 1){
            logger.error { "Failed to update invoice ${invoice.id}" }
            throw DBConnectionException()
        }
        return true
    }
}
