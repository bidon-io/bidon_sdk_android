package org.bidon.gam

import android.app.Activity
import com.google.android.gms.ads.AdSize
import org.bidon.gam.ext.toGamAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

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
        override val adUnit: AdUnit,
    ) : GamBannerAuctionParams {
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        override val price: Double = requireNotNull(adUnit.pricefloor)

        override fun toString(): String {
            return "GamBannerAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val price: Double,
        bidResponse: BidResponse,
    ) : GamBannerAuctionParams {
        override val adUnit: AdUnit = bidResponse.adUnit
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))

        override fun toString(): String {
            return "GamBannerAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}

sealed interface GamFullscreenAdAuctionParams : AdAuctionParams {
    val activity: Activity

    class Network(
        override val activity: Activity,
        override val adUnit: AdUnit,
    ) : GamFullscreenAdAuctionParams {
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        override val price: Double = requireNotNull(adUnit.pricefloor)

        override fun toString(): String {
            return "GamFullscreenAdAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val price: Double,
        bidResponse: BidResponse
    ) : GamFullscreenAdAuctionParams {
        override val adUnit: AdUnit = bidResponse.adUnit
        val adUnitId: String = requireNotNull(adUnit.extra?.getString("ad_unit_id"))
        val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))

        override fun toString(): String {
            return "GamFullscreenAdAuctionParams($adUnitId, bidPrice=$price, payload=${payload.take(20)})"
        }
    }
}
