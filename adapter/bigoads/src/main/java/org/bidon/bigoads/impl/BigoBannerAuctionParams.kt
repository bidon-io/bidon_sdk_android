package org.bidon.bigoads.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

data class BigoBannerAuctionParams(
    val bannerFormat: BannerFormat,
    val slotId: String,
    val bidPrice: Double,
    val payload: String,
) : AdAuctionParams {
    override val price: Double get() = bidPrice
    override val lineItem: LineItem? = null
}

data class BigoFullscreenAuctionParams(
    val slotId: String,
    val bidPrice: Double,
    val payload: String,
) : AdAuctionParams {
    override val price: Double get() = bidPrice
    override val lineItem: LineItem? = null
}
