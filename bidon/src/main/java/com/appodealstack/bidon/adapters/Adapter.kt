package com.appodealstack.bidon.adapters

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.domain.AuctionRequest
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.config.data.models.AdapterInfo
import kotlinx.serialization.json.JsonObject

interface Adapter {
    val demandId: DemandId
    val adapterInfo: AdapterInfo
}

interface Initializable<T : AdapterParameters> {
    suspend fun init(activity: Activity, configParams: T)
    fun parseConfigParam(json: JsonObject): T
}

sealed interface AdSource {
    interface Interstitial<T : AdParams> : AdSource {
        fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: T): AuctionRequest
        fun interstitialParams(
            priceFloor: Double,
            lineItems: List<LineItem>,
        ): AdParams
    }

    interface Rewarded<T : AdParams> : AdSource {
        fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: T): AuctionRequest
        fun rewardedParams(
            priceFloor: Double,
            lineItems: List<LineItem>,
        ): AdParams
    }

    interface Banner<T : AdParams> : AdSource {
        fun banner(context: Context, demandAd: DemandAd, adParams: T): AuctionRequest
        fun bannerParams(
            priceFloor: Double,
            lineItems: List<LineItem>,
            bannerSize: BannerSize,
            adContainer: ViewGroup?
        ): AdParams
    }

    interface AdParams
}