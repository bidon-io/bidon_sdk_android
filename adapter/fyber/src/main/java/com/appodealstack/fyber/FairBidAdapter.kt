package com.appodealstack.fyber

import android.content.Context
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.DemandId

val FairBidDemandId = DemandId("fair_bid")

class FairBidAdapter: Adapter.Mediation<FairBidParameters> {
    override val demandId: DemandId = FairBidDemandId

    override suspend fun init(context: Context, configParams: FairBidParameters) {
    }
}