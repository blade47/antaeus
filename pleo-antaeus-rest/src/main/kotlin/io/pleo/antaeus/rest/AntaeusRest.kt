/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val subscriptionService: SubscriptionService,
    private val planService: PlanService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("subscriptions") {
                        // URL: /rest/v1/subscriptions
                        get {
                            it.json(subscriptionService.fetchAll())
                        }

                        // URL: /rest/v1/subscriptions/{:id}
                        get(":id") {
                            it.json(subscriptionService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("plans") {
                        // URL: /rest/v1/plans
                        get {
                            it.json(planService.fetchAll())
                        }

                        // URL: /rest/v1/plans/{:id}
                        get(":id") {
                            it.json(planService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("startBillingTask") {
                        // URL: /rest/v1/startBillingTask
                        get {
                            billingService.startBillingTask()
                            it.json({ "status" to "started" })
                        }
                    }

                    path("stopBillingTask") {
                        // URL: /rest/v1/stopBillingTask
                        get {
                            billingService.stopBillingTask()
                            it.json({ "status" to billingService.isBillingTaskRunning() })
                        }
                    }

                    path("forceStopBillingTask") {
                        // URL: /rest/v1/forceStopBillingTask
                        get {
                            if (billingService.isBillingTaskRunning()) billingService.forceStopBillingTask()
                            it.json({ "status" to billingService.isBillingTaskRunning() })
                        }
                    }

                    path("isBillingTaskRunning") {
                        // URL: /rest/v1/isBillingTaskRunning
                        get {
                            it.json({ "status" to billingService.isBillingTaskRunning() })
                        }
                    }
                }
            }
        }
    }
}
