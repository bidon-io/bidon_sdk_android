package com.appodealstack.mads.postbid

import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.DemandId

class AdmobDemand : Demand.PostBid {
    override fun createActionRequest(ownerDemandAd: DemandAd): AuctionRequest.PostBid {
        TODO("Not yet implemented")
    }

    override val demandId = DemandId("Admob")

    override suspend fun init(context: Context, configParams: Bundle) {
        TODO("Not yet implemented")
    }
}