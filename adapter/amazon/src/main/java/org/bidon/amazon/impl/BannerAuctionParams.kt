package org.bidon.amazon.impl

import android.app.Activity
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

data class BannerAuctionParams(
    val bannerFormat: BannerFormat,
    val activity: Activity,
    override val adUnit: AdUnit,
    override val price: Double,
) : AdAuctionParams {
    val slotUuid: String = requireNotNull(adUnit.extra?.getString("slot_uuid")) {
        "slotUuid is null"
    }
    val format: SlotType = requireNotNull(
        adUnit.extra?.getString("format")?.let {
            SlotType.getOrNull(it).takeIf { it in arrayOf(SlotType.BANNER, SlotType.MREC) }
        }
    ) {
        "format is null"
    }
}
