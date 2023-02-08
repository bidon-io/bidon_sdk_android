package com.appodealstack.bidon.domain.auction

import com.appodealstack.bidon.domain.common.DemandAd

/**
 * Created by Aleksei Cherniaev on 07/09/2022.
 */
internal interface Auction {
    suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeParamData: AdTypeParam,
    ): Result<List<AuctionResult>>
}

enum class AuctionState {
    Initialized,
    InProgress,
    Finished,
}
