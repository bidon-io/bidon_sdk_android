package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class BigoAdsBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotId: String? = adUnit.extra?.getString("slot_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type
}

internal class BigoAdsFullscreenAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotId: String? = adUnit.extra?.getString("slot_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type
}
