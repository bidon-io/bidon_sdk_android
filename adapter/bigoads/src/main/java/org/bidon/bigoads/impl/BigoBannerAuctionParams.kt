package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

data class BigoBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val payload: String? = adUnit.extra?.getString("payload")
    val slotId: String? = adUnit.extra?.getString("slot_id")
}

data class BigoFullscreenAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val payload: String? = adUnit.extra?.getString("payload")
    val slotId: String? = adUnit.extra?.getString("slot_id")
}
