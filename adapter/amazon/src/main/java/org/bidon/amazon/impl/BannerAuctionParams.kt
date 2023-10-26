package org.bidon.amazon.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class BannerAuctionParams(
    val bannerFormat: BannerFormat,
    val activity: Activity,
    val slotUuid: String,
    override val price: Double
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}
