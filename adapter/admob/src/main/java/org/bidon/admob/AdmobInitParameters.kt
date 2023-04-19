package org.bidon.admob

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val pricefloor: Double,
    val containerWidth: Float,
) : AdAuctionParams {
    override val adUnitId: String?
        get() = lineItem.adUnitId
}

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    val pricefloor: Double
) : AdAuctionParams {
    override val adUnitId: String?
        get() = lineItem.adUnitId
}
