/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.DBConnectionException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceStatusNotFoundException
import io.pleo.antaeus.core.exceptions.SubscriptionStatusNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.*
import mu.KotlinLogging

class InvoiceService(private val dal: InvoiceDal) {

    private val logger = KotlinLogging.logger {}

    fun create(amount: Money, to: Customer) : Invoice {
        return dal.create( Invoice(
                amount = amount,
                customerId = to.id,
                status = dal.getStatus(InvoiceStatuses.PENDING) ?: run { throw InvoiceStatusNotFoundException(InvoiceStatuses.PENDING.toString()) })
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

    fun getStatus(status: InvoiceStatuses): InvoiceStatus {
        return dal.getStatus(status) ?: throw InvoiceStatusNotFoundException(status.toString())
    }

    fun createStatus(status: InvoiceStatuses, description: String): InvoiceStatus{
        return dal.createStatus(status, description) ?: throw DBConnectionException()
    }
}
