package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult

internal interface Auction {
    suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeAdditionalData: AdTypeAdditional
    ): Result<List<AuctionResult>>

    fun destroy()

    val results: List<AuctionResult>
    val isActive: Boolean
}

enum class AuctionState {
    Initialized,
    InProgress,
    Finished,
    Destroyed
}
