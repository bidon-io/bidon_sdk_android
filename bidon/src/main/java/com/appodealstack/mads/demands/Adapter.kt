package com.appodealstack.mads.demands

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.auctions.AuctionRequest

sealed interface Adapter<T: AdapterParameters> {
    val demandId: DemandId

    suspend fun init(context: Context, configParams: T)

    interface Mediation<T: AdapterParameters> : Adapter<T>
    interface PostBid<T: AdapterParameters> : Adapter<T>
}

interface AdapterParameters

sealed interface AdSource {
    interface Interstitial : AdSource {
        fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest
    }

    interface Rewarded : AdSource {
        fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest
    }

    interface Banner : AdSource {
        fun banner(context: Context, demandAd: DemandAd, adParams: Bundle): AuctionRequest
    }
}