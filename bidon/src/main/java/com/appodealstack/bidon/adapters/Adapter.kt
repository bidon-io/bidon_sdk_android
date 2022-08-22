package com.appodealstack.bidon.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.banners.BannerSize
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
    interface Interstitial<T : AdAuctionParams> : AdProvider {
        fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<T>
    }

    interface Banner<T : AdAuctionParams> : AdProvider {
        fun banner(demandAd: DemandAd, roundId: String): AdSource.Banner<T>
    }

    interface Rewarded<T : AdAuctionParams> : AdProvider {
        fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<T>
    }
}

sealed interface AdSource<T : AdAuctionParams> {
    val demandId: DemandId
    val ad: Ad?
    val state: StateFlow<AdState>

    /**
     * Applovin needs Activity instance for interstitial 🤦‍️
     */
    suspend fun bid(adParams: T): Result<AuctionResult>
    suspend fun fill(): Result<Ad>
    fun show(activity: Activity)
    fun destroy()

    interface Interstitial<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
        ): AdAuctionParams
    }

    interface Rewarded<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
        ): AdAuctionParams
    }

    interface Banner<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            bannerSize: BannerSize,
            adContainer: ViewGroup,
        ): AdAuctionParams

        fun getAdView(): View
    }
}

interface AdAuctionParams

interface WinLossNotifiable {
    fun notifyLoss()
    fun notifyWin()
}

sealed interface AdState {
    object Initialized : AdState
    class Expired(val ad: Ad) : AdState
    class Bid(val result: AuctionResult) : AdState
    class LoadFailed(val cause: Throwable) : AdState
    class Fill(val ad: Ad) : AdState
    class Clicked(val ad: Ad) : AdState
    class Closed(val ad: Ad) : AdState
    class Impression(val ad: Ad) : AdState
    class OnReward(val ad: Ad, val reward: Reward) : AdState
    class ShowFailed(val cause: Throwable) : AdState
}