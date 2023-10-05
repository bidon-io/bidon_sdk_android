package org.bidon.sdk.ads.cache

import androidx.annotation.Keep

@Keep
interface Cacheable {
    fun withSettings(settings: Settings)

    @Keep
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
        val useDifferentDemands: Boolean = true,
        val skipCurrentDemand: Boolean = true,

        val useEfficientRound: Boolean = true,
        val useBiddingRound: Boolean = true,

        /**
         * Maximum number of polls per demand.
         * If demand is not limited, then it is not included in the map.
         */
        val maximumPolls: Map<String, Int>
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
            useDifferentDemands = true,
            skipCurrentDemand = true,
            useEfficientRound = true,
            useBiddingRound = true,
            maximumPolls = emptyMap()
        )
    }
}