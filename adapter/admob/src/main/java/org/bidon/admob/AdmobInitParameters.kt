package org.bidon.admob

import android.app.Activity
import com.google.android.gms.ads.AdSize
import org.bidon.admob.ext.toAdmobAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

data class AdmobInitParameters(
    val requestAgent: String?,
    val queryInfoType: String?
) : AdapterParameters

sealed interface AdmobBannerAuctionParams : AdAuctionParams {
    val activity: Activity
    val bannerFormat: BannerFormat
    val containerWidth: Float
    val adSize: AdSize get() = bannerFormat.toAdmobAdSize(activity, containerWidth)

    class Network(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val adUnit: AdUnit,
    ) : AdmobBannerAuctionParams {
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        override val price: Double = requireNotNull(adUnit.pricefloor)

        override fun toString(): String {
            return "AdmobBannerAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val price: Double,
        bidResponse: BidResponse
    ) : AdmobBannerAuctionParams {
        override val adUnit: AdUnit = bidResponse.adUnit
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))

        override fun toString(): String {
            return "AdmobBannerAuctionParams($adUnit, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}

sealed interface AdmobFullscreenAdAuctionParams : AdAuctionParams {
    val activity: Activity

    class Network(
        override val activity: Activity,
        override val adUnit: AdUnit,
    ) : AdmobFullscreenAdAuctionParams {
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        override val price: Double = requireNotNull(adUnit.pricefloor)

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val price: Double,
        bidResponse: BidResponse
    ) : AdmobFullscreenAdAuctionParams {
        override val adUnit: AdUnit = bidResponse.adUnit
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($adUnit, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}
