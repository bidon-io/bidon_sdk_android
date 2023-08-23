package org.bidon.admob

import android.content.Context
import com.google.android.gms.ads.AdSize
import org.bidon.admob.ext.toAdmobAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class AdmobInitParameters(
    val requestAgent: String?,
    val queryInfoType: String?
) : AdapterParameters

sealed interface AdmobBannerAuctionParams : AdAuctionParams {
    val bannerFormat: BannerFormat
    val context: Context
    val containerWidth: Float
    val adSize: AdSize get() = bannerFormat.toAdmobAdSize(context, containerWidth)

    class Network(
        override val context: Context,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        val lineItem: LineItem,
    ) : AdmobBannerAuctionParams {
        override val adUnitId: String = requireNotNull(lineItem.adUnitId)
        override val price: Double get() = lineItem.pricefloor

        override fun toString(): String {
            return "AdmobBannerAuctionParams($lineItem)"
        }
    }

    class Bidding(
        override val context: Context,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val price: Double,
        override val adUnitId: String,
        val payload: String,
    ) : AdmobBannerAuctionParams {

        override fun toString(): String {
            return "AdmobBannerAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}

sealed interface AdmobFullscreenAdAuctionParams : AdAuctionParams {
    val context: Context

    class Network(
        override val context: Context,
        val lineItem: LineItem,
    ) : AdmobFullscreenAdAuctionParams {
        override val adUnitId: String = requireNotNull(lineItem.adUnitId)
        override val price: Double get() = lineItem.pricefloor

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($lineItem)"
        }
    }

    class Bidding(
        override val context: Context,
        override val price: Double,
        override val adUnitId: String,
        val payload: String,
    ) : AdmobFullscreenAdAuctionParams {

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}
