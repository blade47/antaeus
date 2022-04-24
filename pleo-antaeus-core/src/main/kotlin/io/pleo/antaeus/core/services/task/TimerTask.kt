package io.pleo.antaeus.core.services.task

import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TimerTask internal constructor(
        name: String,
        private val delay: Duration = Duration.ZERO,
        private val repeat: Duration? = null,
        action: suspend () -> Unit
) {
    private val logger = KotlinLogging.logger {}
    private val keepRunning = AtomicBoolean(true)
    private var job: Job? = null
    private val tryAction = suspend {
        try {
            action()
        } catch (e: Throwable) {
            logger.warn { "$name timer action failed: $action" }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun start()  {
        job = GlobalScope.launch() {
            delay(delay)
            if (repeat != null) {
                while (keepRunning.get()) {
                    val beginTime = System.currentTimeMillis()
                    tryAction()
                    val duration = System.currentTimeMillis() - beginTime
                    val adjustedRepeatingDelay = repeat.minus(duration.toDuration(DurationUnit.MILLISECONDS))
                    delay(adjustedRepeatingDelay)
                }
            } else {
                if (keepRunning.get()) {
                    tryAction()
                }
            }
        }
    }

    /**
     * Initiates an orderly shutdown, where if the timer task is currently running,
     * we will let it finish, but not run it again.
     * Invocation has no additional effect if already shut down.
     */
    fun shutdown() {
        keepRunning.set(false)
    }

    fun isRunning(): Boolean {
        return job?.isActive ?: false
    }

    /**
     * Immediately stops the timer task, even if the job is currently running,
     * by cancelling the underlying SubscriptionTimerTask Job.
     */
    fun cancel() {
        shutdown()
        job?.cancel()
    }

    companion object {
        /**
         * Runs the given `action` after the given `delay`,
         * once the `action` completes, waits the `repeat` duration
         * and runs again, until `shutdown` is called.
         *
         * if action() throws an exception, it will be swallowed and a warning will be logged.
         */
        fun start(
                name: String,
                delay: Duration = Duration.ZERO,
                repeat: Duration? = null,
                action: suspend () -> Unit
        ): TimerTask =
                TimerTask(name, delay, repeat, action).also { it.start() }
    }
}