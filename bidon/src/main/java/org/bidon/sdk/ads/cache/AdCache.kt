package org.bidon.sdk.ads.cache

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult

internal interface AdCache {
    val demandAd: DemandAd

    fun cache(adTypeParam: AdTypeParam)

    /**
     * Exposes only
     */
    fun peek(): AuctionResult?

    /**
     * Removes from cache and exposes
     */
    suspend fun poll(): AuctionResult

    fun clear()

    companion object {
        const val MIN_CACHE_TIMEOUT = 5000L
    }
}