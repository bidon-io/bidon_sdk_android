package org.bidon.admob

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

object AdmobInitParameters : AdapterParameters

class AdmobBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val containerWidth: Float,
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val pricefloor: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "AdmobBannerAuctionParams($lineItem)"
    }
}

class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val pricefloor: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "AdmobFullscreenAdAuctionParams(lineItem=$lineItem)"
    }
}
