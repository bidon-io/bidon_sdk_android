package org.bidon.inmobi.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

class InmobiBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val price: Double,
    override val lineItem: LineItem
) : AdAuctionParams {
    val placementId: Long = requireNotNull(lineItem.adUnitId).toLong()

    override fun toString(): String {
        return "InmobiBannerAuctionParams($bannerFormat, placementId=$placementId, price=$price)"
    }
}
