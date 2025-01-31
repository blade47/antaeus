package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.services.task.TimerTask
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TimerTaskTest() {
    @Test
    fun `Basic operation`() {
        var times = 0
        val task = TimerTask.create("test", repeat = 100.milliseconds) {
            times++
        }
        task.start()
        Thread.sleep(1_000)
        task.shutdown()
        Thread.sleep(1_000)
        assert(times == 10)
        assert(! task.isRunning())
    }

    @Test
    fun `cancel during task run`() {
        val task = TimerTask.create("test", repeat = 100.milliseconds) {
            println("Task running...")
        }
        task.start()
        Thread.sleep(1_000)
        task.shutdown()
        Thread.sleep(1_000)
        assert(! task.isRunning())
    }

    @Test
    fun `shutdown and restart task`() {
        var times = 0
        val task = TimerTask.create("test", repeat = 100.milliseconds) {
            times++
        }
        task.start()
        Thread.sleep(1_000)
        task.shutdown()
        Thread.sleep(1_000)
        assert(! task.isRunning())
        assert(times == 10)
        task.start()
        Thread.sleep(1_000)
        task.shutdown()
        Thread.sleep(1_000)
        assert(! task.isRunning())
        assert(times == 10)
    }

    @Test
    @Disabled
    fun `delay between calls`() {
        val deltas = mutableListOf<Duration>()
        var startedAt: Long? = null
        val task = TimerTask.create("test", repeat = 2000.milliseconds) {
            val newStarting = System.currentTimeMillis()
            startedAt?.let {
                val delta = newStarting - it
                deltas.add(delta.toDuration(DurationUnit.MILLISECONDS))
            }
            startedAt = newStarting
            delay(1000.milliseconds)
        }
        task.start()
        Thread.sleep(20_000)
        task.shutdown()
        Thread.sleep(2_000)

        assert(! task.isRunning())
        assert(deltas.size == 9)

        deltas.forEach { delta -> assert(delta.inWholeSeconds == 2.seconds.inWholeSeconds) }
    }

    @Test
    @Disabled
    fun `delay between calls with internal function heavier than repeating time`() {
        val deltas = mutableListOf<Duration>()
        var startedAt: Long? = null
        val task = TimerTask.create("test", repeat = 1000.milliseconds) {
            val newStarting = System.currentTimeMillis()
            startedAt?.let {
                val delta = newStarting - it
                deltas.add(delta.toDuration(DurationUnit.MILLISECONDS))
            }
            startedAt = newStarting
            delay(2000.milliseconds)
        }
        task.start()
        Thread.sleep(20_000)
        task.shutdown()
        Thread.sleep(2_000)

        assert(! task.isRunning())
        assert(deltas.size == 9)
        // Because the internal function delay is greater than repeating delay, we ignore the latter
        deltas.forEach { delta -> assert(delta.inWholeSeconds == 2.seconds.inWholeSeconds) }
    }
}
