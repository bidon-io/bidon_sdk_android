package org.bidon.admob

import android.app.Activity
import com.google.android.gms.ads.AdSize
import org.bidon.admob.ext.toAdmobAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class AdmobInitParameters(
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
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")

        override fun toString(): String {
            return "AdmobBannerAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val bannerFormat: BannerFormat,
        override val containerWidth: Float,
        override val adUnit: AdUnit
    ) : AdmobBannerAuctionParams {
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")
        val payload: String? = adUnit.extra?.getString("payload")

        override fun toString(): String {
            return "AdmobBannerAuctionParams($adUnit, bidPrice=$price, payload=${payload?.take(20)})"
        }
    }
}

sealed interface AdmobFullscreenAdAuctionParams : AdAuctionParams {
    val activity: Activity

    class Network(
        override val activity: Activity,
        override val adUnit: AdUnit,
    ) : AdmobFullscreenAdAuctionParams {
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($adUnit)"
        }
    }

    class Bidding(
        override val activity: Activity,
        override val adUnit: AdUnit
    ) : AdmobFullscreenAdAuctionParams {
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")
        val payload: String? = adUnit.extra?.getString("payload")

        override fun toString(): String {
            return "AdmobFullscreenAdAuctionParams($adUnit, bidPrice=$price, payload=${payload?.take(20)})"
        }
    }
}
