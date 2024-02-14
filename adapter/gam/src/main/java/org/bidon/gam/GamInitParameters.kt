package org.bidon.gam

import android.app.Activity
import com.google.android.gms.ads.AdSize
import org.bidon.gam.ext.toGamAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class GamInitParameters(
    val requestAgent: String?,
    val queryInfoType: String?
) : AdapterParameters

sealed interface GamBannerAuctionParams : AdAuctionParams {
    val activity: Activity
    val bannerFormat: BannerFormat
    val containerWidth: Float
    val adSize: AdSize get() = bannerFormat.toGamAdSize(activity, containerWidth)

    class Network(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val lineItem: LineItem,
    ) : GamBannerAuctionParams {
        val adUnitId: String = requireNotNull(lineItem.adUnitId)
        override val price: Double get() = lineItem.pricefloor

        override fun toString(): String {
            return "GamBannerAuctionParams($lineItem)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val price: Double,
        val adUnitId: String,
        val payload: String,
    ) : GamBannerAuctionParams {
        override val lineItem: LineItem? = null

        override fun toString(): String {
            return "GamBannerAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}

sealed interface GamFullscreenAdAuctionParams : AdAuctionParams {
    val activity: Activity

    class Network(
        override val activity: Activity,
        override val lineItem: LineItem,
    ) : GamFullscreenAdAuctionParams {
        val adUnitId: String = requireNotNull(lineItem.adUnitId)
        override val price: Double get() = lineItem.pricefloor

        override fun toString(): String {
            return "GamFullscreenAdAuctionParams($lineItem)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val price: Double,
        val adUnitId: String,
        val payload: String,
    ) : GamFullscreenAdAuctionParams {
        override val lineItem: LineItem? = null

        override fun toString(): String {
            return "GamFullscreenAdAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}
