package com.appodealstack.bidon.adapters

import android.app.Activity
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.config.data.models.AdapterInfo
import kotlinx.coroutines.flow.StateFlow
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
    val ad: Ad?
    fun destroy()

    interface Interstitial<T : AdParams> : AdSource {
        val state: StateFlow<State>

        // todo a28 check bid-> context instead of activity
        fun getParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdParams
        suspend fun bid(activity: Activity?, adParams: T): Result<State.Bid.Success>
        suspend fun fill(): Result<State.Fill.Success>
        fun show(activity: Activity)
        fun notifyLoss()
        fun notifyWin()

        sealed interface State {
            object Initialized : State
            class Expired(val ad: Ad) : State

            sealed interface Bid : State {
                object Requesting : Bid
                class Success(val result: AuctionResult) : Bid
                class Failure(val cause: Throwable) : Bid
            }

            sealed interface Fill : State {
                object LoadingResources : Fill
                class Success(val ad: Ad) : Fill
                class Failure(val cause: Throwable) : Fill
            }

            sealed interface Show : State {
                class ShowFailed(val cause: Throwable) : Show
                class Impression(val ad: Ad) : Show
                class Clicked(val ad: Ad) : Show
                class Closed(val ad: Ad) : Show
            }
        }
    }
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