package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.PlanNotFoundException
import io.pleo.antaeus.data.PlanDal
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EmbeddedDb::class)
class PlanServiceTest(db: Database){

    private val planService = PlanService(dal = PlanDal(db = db))

    @Test
    fun `should have a valid connection`() {
        Assertions.assertNotNull(planService)
    }

    @Test
    fun `will throw if Plan is not found`() {
        assertThrows<PlanNotFoundException> {
            planService.fetch(404)
        }
    }
}
