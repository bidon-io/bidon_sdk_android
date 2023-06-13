package org.bidon.sdk.auction

import org.bidon.sdk.adapter.DemandAd

/**
 * Created by Bidon Team on 07/09/2022.
 */
internal interface Auction {
    suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        adTypeParamData: AdTypeParam,
    ): Result<List<AuctionResult>>
}

enum class AuctionState {
    Initialized,
    InProgress,
    Finished,
}
