package org.bidon.inmobi.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

class InmobiBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = requireNotNull(adUnit.pricefloor)
    val placementId: Long = requireNotNull(adUnit.extra?.optLong("placement_id")).toLong()

    override fun toString(): String {
        return "InmobiBannerAuctionParams($bannerFormat, placementId=$placementId, price=$price)"
    }
}
