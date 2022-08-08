package com.appodealstack.bidon.demands

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.auctions.AuctionRequest

sealed interface Adapter<T : AdapterParameters> {
    val demandId: DemandId

    suspend fun init(activity: Activity, configParams: T)

    interface Mediation<T : AdapterParameters> : Adapter<T> {
        val mediationNetwork: BNMediationNetwork
    }
    interface PostBid<T : AdapterParameters> : Adapter<T>
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
        fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest
    }
}