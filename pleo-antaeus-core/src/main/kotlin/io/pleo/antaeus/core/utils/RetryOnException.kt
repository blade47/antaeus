package io.pleo.antaeus.core.utils

/**
 * Encapsulates retry-on-exception operations
 */
class RetryOnException

    constructor(private var numRetries: Int = DEFAULT_RETRIES,
                private val timeToWaitMS: Long = DEFAULT_TIME_TO_WAIT_MS
    ) {
    /**
     * shouldRetry
     * Returns true if a retry can be attempted.
     * @return  True if retries attempts remain; else false
     */
    private fun shouldRetry(): Boolean {
        return numRetries >= 0
    }

    /**
     * waitUntilNextTry
     * Waits for timeToWaitMS. Ignores any interrupted exception
     */
    private fun waitUntilNextTry() {
        try {
            Thread.sleep(timeToWaitMS)
        } catch (_: InterruptedException) {
        }
    }

    fun resetCounter() {
        numRetries = DEFAULT_RETRIES
    }

    /**
     * exceptionOccurred
     * Call when an exception has occurred in the block. If the
     * retry limit is exceeded, throws an exception.
     * Else waits for the specified time.
     * @throws Exception
     */
    fun exceptionOccurred(e: Exception) {
        numRetries--
        if (!shouldRetry()) {
            throw e
        }
        waitUntilNextTry()
    }

    companion object {
        const val DEFAULT_RETRIES = 3
        const val DEFAULT_TIME_TO_WAIT_MS: Long = 2000
    }
}