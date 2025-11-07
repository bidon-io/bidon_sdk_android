package org.bidon.amazon.impl

import android.app.Activity
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal data class BannerAuctionParams(
    val bannerFormat: BannerFormat,
    val activity: Activity,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotUuid: String? = adUnit.extra?.getString("slot_uuid")
    val format: SlotType? =
        adUnit.extra?.getString("format")?.let {
            SlotType.getOrNull(it).takeIf { it in arrayOf(SlotType.BANNER, SlotType.MREC) }
        }
}
