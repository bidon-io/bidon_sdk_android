package org.bidon.sdk.ads.cache

interface Cacheable {
    fun withSettings(settings: Settings)

    data class Settings(
        val minCacheSize: Int,
        val cacheCapacity: Int,
        /**
         * Timeout is doubled each time when no ad is loaded.
         */
        val minCacheTimeoutMs: Long,
        val maxCacheTimeoutMs: Long,
        /**
         * Use different demands if possible. Need to be A/B-tested.
         */
        val useDifferentDemands: Boolean = false,
        val skipCurrentDemand: Boolean = false,
    )

    companion object {
        private const val MIN_CACHE_SIZE = 1
        private const val CACHE_CAPACITY = 4
        private const val START_RETRY_TIMEOUT = 1000L
        private const val MAX_CACHE_TIMEOUT = 60_000L

        val DefaultSettings get() = Settings(
            minCacheSize = MIN_CACHE_SIZE,
            cacheCapacity = CACHE_CAPACITY,
            minCacheTimeoutMs = START_RETRY_TIMEOUT,
            maxCacheTimeoutMs = MAX_CACHE_TIMEOUT,
            useDifferentDemands = false,
            skipCurrentDemand = false,
        )
    }
}