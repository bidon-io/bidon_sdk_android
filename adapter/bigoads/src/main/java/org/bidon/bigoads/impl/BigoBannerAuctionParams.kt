package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat

data class BigoBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    val slotId: String,
    val bidPrice: Double,
    val payload: String,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
    override val price: Double get() = bidPrice
}

data class BigoFullscreenAuctionParams(
    val slotId: String,
    val bidPrice: Double,
    val payload: String,
) : AdAuctionParams {
    override val adUnitId: String get() = slotId
    override val price: Double get() = bidPrice
}
