package org.bidon.mintegral

import android.app.Activity
import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 */
class MintegralAuctionParam(
    val activity: Activity,
    override val price: Double,
    val payload: String,
    val unitId: String?,
    val placementId: String?,
) : AdAuctionParams {
    override val lineItem: LineItem? = null
    override fun toString(): String {
        return "MintegralAuctionParam(price=$price, adUnitId=$lineItem, placementId=$placementId, payload='$payload')"
    }
}

class MintegralBannerAuctionParam(
    val context: Context,
    val bannerFormat: BannerFormat,
    override val price: Double,
    val payload: String,
    val unitId: String?,
    val placementId: String?,
) : AdAuctionParams {
    override val lineItem: LineItem? = null
    override fun toString(): String {
        return "MintegralBannerAuctionParam($bannerFormat, price=$price, adUnitId=$lineItem, placementId=$placementId, payload='$payload')"
    }
}