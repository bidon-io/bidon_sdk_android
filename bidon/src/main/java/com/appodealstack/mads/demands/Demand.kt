package com.appodealstack.mads.demands

import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.auctions.AuctionRequest

sealed interface Demand {
    val demandId: DemandId

    suspend fun init(context: Context, configParams: Bundle)

    interface Mediation : Demand {
        fun createAuctionRequest(demandAd: DemandAd): AuctionRequest.Mediation
    }

    interface PostBid : Demand {
        fun createActionRequest(): AuctionRequest.PostBid
    }
}