package org.bidon.applovin

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class ApplovinParameters(
    val key: String,
) : AdapterParameters

class ApplovinBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val lineItem: LineItem,
) : AdAuctionParams {
    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "ApplovinBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$lineItem)"
    }
}

class ApplovinFullscreenAdAuctionParams(
    override val lineItem: LineItem,
    val timeoutMs: Long
) : AdAuctionParams {
    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "ApplovinFullscreenAdAuctionParams(timeoutMs=$timeoutMs, lineItem=$lineItem)"
    }
}
