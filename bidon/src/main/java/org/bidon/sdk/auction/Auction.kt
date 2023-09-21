package org.bidon.sdk.auction

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.stats.models.BidStat

/**
 * Created by Bidon Team on 07/09/2022.
 */
internal interface Auction {
    fun start(
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
        existing: Map<DemandId, BidStat>,
        onSuccess: (results: List<AuctionResult>) -> Unit,
        onFailure: (Throwable) -> Unit,
        /**
         * Calls on each success round results
         */
        onEach: (roundResults: List<AuctionResult>) -> Unit = {},
    )

    /**
     * Cancel auction in progress and sent /stats
     */
    fun cancel()

    enum class AuctionState {
        Initialized,
        InProgress,
        Finished,
    }
}
