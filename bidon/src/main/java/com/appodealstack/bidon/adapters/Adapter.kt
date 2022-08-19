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
        fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<T>
    }
}

sealed interface AdSource<T : AdSource.AdParams> {
    val demandId: DemandId
    val ad: Ad?

    /**
     * Applovin needs Activity instance for interstitial 🤦‍️
     */
    suspend fun bid(activity: Activity?, adParams: T): Result<AuctionResult>
    suspend fun fill(): Result<Ad>
    fun show(activity: Activity)
    fun destroy()
    fun getParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdParams


    interface Interstitial<T : AdParams> : AdSource<T> {
        val state: StateFlow<State>
    }

    interface Rewarded<T : AdParams> : AdSource<T> {
        val state: StateFlow<State>

        sealed interface OnReward : State {
            class Success(val ad: Ad, val reward: Reward) : OnReward
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
            class Clicked(val ad: Ad) : Show
            class Closed(val ad: Ad) : Show
            class Impression(val ad: Ad) : Show
        }
    }
}

interface WinLossNotifiable {
    fun notifyLoss()
    fun notifyWin()
}

sealed interface AdObjectState {
    object Initialized : AdObjectState
    class Expired(val ad: Ad) : AdObjectState
    class Bid(val result: AuctionResult) : AdObjectState
    class Fill(val ad: Ad) : AdObjectState
    class Clicked(val ad: Ad) : AdObjectState
    class Closed(val ad: Ad) : AdObjectState
    class Impression(val ad: Ad) : AdObjectState
    class OnReward(val ad: Ad, val reward: Reward) : AdObjectState
}