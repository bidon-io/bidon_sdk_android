package org.bidon.applovin

import android.app.Activity
import com.applovin.sdk.AppLovinAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

internal class ApplovinParameters(
    val key: String,
) : AdapterParameters

internal class ApplovinBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val zoneId: String? = adUnit.extra?.getString("zone_id")

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> AppLovinAdSize.MREC
            BannerFormat.Banner -> AppLovinAdSize.BANNER
            BannerFormat.LeaderBoard -> AppLovinAdSize.LEADER
            BannerFormat.Adaptive -> if (isTablet) AppLovinAdSize.LEADER else AppLovinAdSize.BANNER
        }

    override fun toString(): String {
        return "ApplovinBannerAuctionParams(bannerFormat=$bannerFormat, adUnit=$adUnit)"
    }
}

internal class ApplovinFullscreenAdAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val zoneId: String? = adUnit.extra?.getString("zone_id")

    override fun toString(): String {
        return "ApplovinFullscreenAdAuctionParams(adUnit=$adUnit)"
    }
}
