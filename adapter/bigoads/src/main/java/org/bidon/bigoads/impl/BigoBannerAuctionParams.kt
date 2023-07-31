package org.bidon.bigoads.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat

data class BigoBannerAuctionParams(
    val bannerFormat: BannerFormat,
    val payload: String,
    val slotId: String,
    val bidPrice: Double,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
    override val pricefloor: Double get() = bidPrice
}

data class BigoFullscreenAuctionParams(
    val payload: String,
    val slotId: String,
    val bidPrice: Double,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
    override val pricefloor: Double get() = bidPrice
}
