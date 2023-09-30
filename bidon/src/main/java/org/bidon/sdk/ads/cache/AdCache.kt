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
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    )

    /**
     * Exposes only
     */
    fun peek(): AuctionResult?

    /**
     * Removes from cache if exists and exposes
     */
    fun pop(): AuctionResult?

    /**
     * Removes from cache and exposes
     */
    suspend fun poll(): AuctionResult

    fun clear(onAuctionCancelled: () -> Unit)
}
