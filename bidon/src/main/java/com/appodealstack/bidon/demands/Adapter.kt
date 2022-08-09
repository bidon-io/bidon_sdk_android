package com.appodealstack.bidon.demands

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.auctions.AuctionRequest
import com.appodealstack.bidon.config.domain.AdapterInfo

interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo
}

interface AdapterParameters

interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
}

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