package org.bidon.sdk.ads.cache

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult

internal interface AdCache : Cacheable {
    val demandAd: DemandAd

    /**
     * Caches ads.
     * @param onEach is called when each ad is loaded
     */
    fun cache(
        adTypeParam: AdTypeParam,
        onEach: (AuctionResult) -> Unit
    )

    /**
     * Exposes only
     */
    fun peek(): AuctionResult?

    /**
     * Removes from cache and exposes
     */
    suspend fun poll(): AuctionResult

    /**
     * Use different demands if possible. Need to be A/B-tested.
     */
    fun useDifferentDemands()

    fun clear()

    companion object {
        const val MIN_CACHE_SIZE = 1
        const val CACHE_CAPACITY = 4
        const val MIN_CACHE_TIMEOUT = 1000L
    }
}