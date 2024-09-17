package org.bidon.applovin

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class ApplovinParameters(
    val key: String,
) : AdapterParameters

class ApplovinBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val zoneId: String? = adUnit.extra?.getString("zone_id")

    override fun toString(): String {
        return "ApplovinBannerAuctionParams(bannerFormat=$bannerFormat, adUnit=$adUnit)"
    }
}

class ApplovinFullscreenAdAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val zoneId: String? = adUnit.extra?.getString("zone_id")

    override fun toString(): String {
        return "ApplovinFullscreenAdAuctionParams(adUnit=$adUnit)"
    }
}
