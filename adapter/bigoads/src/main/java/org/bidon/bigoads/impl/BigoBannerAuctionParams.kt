package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

data class BigoBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    val bidPrice: Double,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double get() = bidPrice
    val payload: String = requireNotNull(adUnit.extra?.getString("payload"))
    val slotId: String = requireNotNull(adUnit.extra?.getString("slotId"))
}

data class BigoFullscreenAuctionParams(
    val bidPrice: Double,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double get() = bidPrice
    val payload: String = requireNotNull(adUnit.extra?.getString("payload"))
    val slotId: String = requireNotNull(adUnit.extra?.getString("slotId"))
}
