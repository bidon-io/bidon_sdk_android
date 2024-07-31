package org.bidon.sdk.auction

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Bidon Team on 07/09/2022.
 */
internal interface Auction {
    fun start(
        demandAd: DemandAd,
        adTypeParam: AdTypeParam,
        onSuccess: (winners: List<AuctionResult>, auctionInfo: AuctionInfo) -> Unit,
        onFailure: (AuctionInfo?, Throwable) -> Unit
    )

    /**
     * Cancel auction in progress and sent /stats
     */
    fun cancel(onFailure: (AuctionInfo?, Throwable) -> Unit)

    enum class AuctionState {
        Initialized,
        InProgress,
        Finished,
    }
}
