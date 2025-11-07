package org.bidon.gam

import android.app.Activity
import com.google.android.gms.ads.AdSize
import org.bidon.gam.ext.toGamAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class GamInitParameters(
    val requestAgent: String?,
    val queryInfoType: String?
) : AdapterParameters

internal sealed interface GamBannerAuctionParams : AdAuctionParams {
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
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")

        override fun toString(): String {
            return "GamBannerAuctionParams($adUnit)"
        }
    }
}

internal sealed interface GamFullscreenAdAuctionParams : AdAuctionParams {
    val activity: Activity

    class Network(
        override val activity: Activity,
        override val adUnit: AdUnit,
    ) : GamFullscreenAdAuctionParams {
        override val price: Double = adUnit.pricefloor
        val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")

        override fun toString(): String {
            return "GamFullscreenAdAuctionParams($adUnit)"
        }
    }
}
