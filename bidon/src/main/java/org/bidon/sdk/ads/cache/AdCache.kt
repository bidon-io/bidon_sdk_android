package org.bidon.sdk.ads.cache

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
internal interface AdCache {
    val demandAd: DemandAd

    fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (BidonError) -> Unit,
    )

    /**
     * Exposes only
     */
    fun peek(): AuctionResult?

    /**
     * Removes from cache and exposes
     */
    fun poll(): AuctionResult?

    fun clear()

    companion object {
        const val CacheItemToStartLoading = 3
        const val CacheCapacity = 6
    }
}

internal interface AdCache2 {
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
        const val CacheItemToStartLoading = 3
        const val CacheCapacity = 10
    }
}
