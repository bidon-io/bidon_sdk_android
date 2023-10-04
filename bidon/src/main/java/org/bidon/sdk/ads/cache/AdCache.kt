package org.bidon.sdk.ads.cache

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Aleksei Cherniaev on 28/09/2023.
 */
internal interface AdCache : Cacheable {
    val demandAd: DemandAd

    /**
     * Caches ads.
     */
    fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    )

    /**
     * Exposes only, if exists
     */
    fun peek(): AuctionResult?

    /**
     * Removes from cache if exists and exposes
     */
    fun pop(): AuctionResult?

    /**
     * Waits for the first loaded, then removes from cache and exposes
     */
    suspend fun poll(): AuctionResult

    fun clear()
}