package org.bidon.bigoads.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat

data class BigoBannerAuctionParams(
    val bannerFormat: BannerFormat,
    val payload: String,
    val slotId: String,
    override val pricefloor: Double,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
}

data class BigoFullscreenAuctionParams(
    val payload: String,
    val slotId: String,
    override val pricefloor: Double,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
}
