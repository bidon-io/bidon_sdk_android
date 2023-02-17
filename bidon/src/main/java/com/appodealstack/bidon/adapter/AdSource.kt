package com.appodealstack.bidon.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import kotlinx.coroutines.flow.Flow

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdSource<T : AdAuctionParams> {
    val demandId: DemandId
    val ad: Ad?
    val adEvent: Flow<AdEvent>

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
            bannerFormat: BannerFormat,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>

        fun getAdView(): AdViewHolder
    }
}

class AdViewHolder(val networkAdview: View, val widthPx: Int, val heightPx: Int)
