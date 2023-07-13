package org.bidon.sdk.adapter

import android.app.Activity
import android.view.View
import kotlinx.coroutines.flow.Flow
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.stats.StatisticsCollector

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed interface AdSource<T : AdAuctionParams> : StatisticsCollector {
    val demandId: DemandId
    val ad: Ad?
    val adEvent: Flow<AdEvent>
    val isAdReadyToShow: Boolean

    fun bid(adParams: T)
    fun fill()
    /**
     * Applovin needs Activity instance for interstitial 🤦‍️
     */
    fun show(activity: Activity)
    fun destroy()

    interface Interstitial<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            pricefloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>
    }

    interface Rewarded<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            pricefloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            onLineItemConsumed: (LineItem) -> Unit
        ): Result<AdAuctionParams>
    }

    interface Banner<T : AdAuctionParams> : AdSource<T> {
        fun getAuctionParams(
            activity: Activity,
            pricefloor: Double,
            timeout: Long,
            lineItems: List<LineItem>,
            bannerFormat: BannerFormat,
            onLineItemConsumed: (LineItem) -> Unit,
            containerWidth: Float,
        ): Result<AdAuctionParams>

        fun getAdView(): AdViewHolder?
    }
}

class AdViewHolder(val networkAdview: View, val widthDp: Int, val heightDp: Int)
