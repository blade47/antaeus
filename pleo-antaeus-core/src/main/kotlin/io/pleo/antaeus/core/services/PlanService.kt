/*
    Implements endpoints related to plans.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.DBConnectionException
import io.pleo.antaeus.core.exceptions.PlanNotFoundException
import io.pleo.antaeus.data.PlanDal
import io.pleo.antaeus.models.Plan
import io.pleo.antaeus.models.PlanDescription
import mu.KotlinLogging

class PlanService(private val dal: PlanDal) {

    private val logger = KotlinLogging.logger {}
    fun create(plan: Plan): Plan {
        return dal.create(plan)
            ?: run {
                logger.error { "Failed to create new plan." }
                throw DBConnectionException()
            }
    }

    fun fetchAll(): List<Plan> {
        return dal.fetchAll()
    }

    fun fetch(id: Int): Plan {
        return dal.fetch(id) ?: throw PlanNotFoundException(id)
    }

    fun fetchFirst(): Plan {
        return dal.fetchAll().first()
    }

    fun getByDescription(description: PlanDescription): Plan {
        return dal.fetchByDescription(description.toString()) ?: throw PlanNotFoundException(0)
    }
}