package com.appodealstack.bidon.domain.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.BannerSize
import com.appodealstack.bidon.domain.common.DemandId
import kotlinx.coroutines.flow.SharedFlow

sealed interface AdSource<T : AdAuctionParams> {
    val demandId: DemandId
    val ad: Ad?
    val adState: SharedFlow<AdState>

    /**
     * Applovin needs Activity instance for interstitial 🤦‍️
     */
    suspend fun bid(adParams: T): AuctionResult
    suspend fun fill(): Result<Ad>
    fun show(activity: Activity)
    fun destroy()

    interface Interstitial<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>
    }

    interface Rewarded<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>
    }

    interface Banner<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            adContainer: ViewGroup,
            priceFloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            bannerSize: BannerSize,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>

        fun getAdView(): View
    }
}