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

    fun peek(): AuctionResult?
    fun poll(): AuctionResult?

    fun clear()

    companion object {
        const val CacheItemToStartLoading = 3
        const val CacheCapacity = 6
    }
}

