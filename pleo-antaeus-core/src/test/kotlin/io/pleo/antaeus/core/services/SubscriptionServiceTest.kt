package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.data.SubscriptionDal
import io.pleo.antaeus.data.SubscriptionStatusDal
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EmbeddedDb::class)
class SubscriptionServiceTest(db: Database){

    private val subscriptionService = SubscriptionService(dal = SubscriptionDal(db = db))

    @Test
    fun `should have a valid connection`() {
        Assertions.assertNotNull(subscriptionService)
    }

    @Test
    fun `will throw if Subscription is not found`() {
        assertThrows<SubscriptionNotFoundException> {
            subscriptionService.fetch(404)
        }
    }
}
