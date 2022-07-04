package com.appodealstack.mads.auctions

sealed interface AuctionRequest {
    interface Mediation : AuctionRequest {
        suspend fun execute(): AuctionData
    }

    interface PostBid : AuctionRequest {
        suspend fun execute(additionalData: AdditionalData?): AuctionData
    }

    data class AdditionalData(val priceFloor: Double)
}

