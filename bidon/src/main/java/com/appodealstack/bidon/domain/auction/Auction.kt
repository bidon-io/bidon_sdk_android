package com.appodealstack.bidon.domain.auction

import com.appodealstack.bidon.domain.common.DemandAd

internal interface Auction {
    suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeParamData: AdTypeParam
    ): Result<List<AuctionResult>>
}

enum class AuctionState {
    Initialized,
    InProgress,
    Finished,
}
