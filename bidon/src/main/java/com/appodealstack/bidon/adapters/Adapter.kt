package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.auctions.data.models.AuctionResult
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

sealed interface AdProvider {
    interface Interstitial<T : AdSource.AdParams> : AdProvider {
        fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<T>
    }
    interface Banner<T : AdSource.AdParams> : AdProvider {
        fun banner(demandAd: DemandAd, roundId: String): AdSource.Interstitial<T>
    }
    interface Rewarded<T : AdSource.AdParams> : AdProvider {
        fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Interstitial<T>
    }
}

sealed interface AdSource {
    val demandId: DemandId

    interface Interstitial<T : AdParams> : AdSource {
        // todo a28 check bid-> context instead of activity
        suspend fun bid(activity: Activity?, adParams: T): Result<State.Bid>
        fun getParams(
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
        ): AdParams

        suspend fun fill(): Result<State.Fill>
        fun show(activity: Activity)
        fun notifyLoss()
        fun notifyWin()

        sealed interface State {
            object Initialized : State
            object Requesting : State
            class Bid(val result: AuctionResult) : State
            object LoadingResources : State
            class Fill(val ad: Ad) : State
            class Clicked(val ad: Ad) : State
            class Closed(val ad: Ad) : State
            class Impression(val ad: Ad) : State
            class Shown(val ad: Ad) : State
            class Failed(val cause: Throwable) : State
        }
    }

//    @Deprecated("")
//    interface OldInterstitial<T : AdParams> : AdSource {
//        fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: T): OldAuctionRequest
//        fun interstitialParams(
//            priceFloor: Double,
//            timeout: Long,
//            lineItems: List<LineItem>,
//        ): AdParams
//    }
//
//    @Deprecated("")
//    interface OldRewarded<T : AdParams> : AdSource {
//        fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: T): OldAuctionRequest
//        fun rewardedParams(
//            priceFloor: Double,
//            timeout: Long,
//            lineItems: List<LineItem>,
//        ): AdParams
//    }
//
//    @Deprecated("")
//    interface OldBanner<T : AdParams> : AdSource {
//        fun banner(context: Context, demandAd: DemandAd, adParams: T): OldAuctionRequest
//        fun bannerParams(
//            priceFloor: Double,
//            lineItems: List<LineItem>,
//            bannerSize: BannerSize,
//            adContainer: ViewGroup?
//        ): AdParams
//    }

    interface AdParams
}