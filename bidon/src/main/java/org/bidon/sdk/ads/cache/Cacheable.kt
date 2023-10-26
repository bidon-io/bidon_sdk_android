package org.bidon.sdk.ads.cache

import androidx.annotation.Keep

/**
 * Created by Aleksei Cherniaev on 28/09/2023.
 */
@Keep
interface Cacheable {
    fun withSettings(settings: Settings)

    @Keep
    data class Settings(
        val minCacheSize: Int,
        val cacheCapacity: Int,
    )

    companion object {
        private const val MIN_CACHE_SIZE = 1
        private const val CACHE_CAPACITY = 1

        val DefaultSettings
            get() = Settings(
                minCacheSize = MIN_CACHE_SIZE,
                cacheCapacity = CACHE_CAPACITY,
            )
    }
}