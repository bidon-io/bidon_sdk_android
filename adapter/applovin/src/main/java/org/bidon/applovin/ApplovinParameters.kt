package org.bidon.applovin

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class ApplovinParameters(
    val key: String,
) : AdapterParameters

class ApplovinBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val pricefloor: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "ApplovinBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$lineItem)"
    }
}

class ApplovinFullscreenAdAuctionParams(
    val lineItem: LineItem,
    val timeoutMs: Long
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val pricefloor: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "ApplovinFullscreenAdAuctionParams(timeoutMs=$timeoutMs, lineItem=$lineItem)"
    }
}
