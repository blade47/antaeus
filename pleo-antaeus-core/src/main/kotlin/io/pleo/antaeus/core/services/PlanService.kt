/*
    Implements endpoints related to plans.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.PlanNotFoundException
import io.pleo.antaeus.data.PlanDal
import io.pleo.antaeus.models.Plan
import io.pleo.antaeus.models.PlanDescription

class PlanService(private val dal: PlanDal) {
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
        return dal.fetchBy(description.toString()) ?: throw PlanNotFoundException(0)
    }
}