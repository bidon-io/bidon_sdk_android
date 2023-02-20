package com.appodealstack.bidon.auction

import com.appodealstack.bidon.adapter.DemandAd
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Aleksei Cherniaev on 07/09/2022.
 */
internal interface Auction {
    val state: StateFlow<AuctionState>

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
