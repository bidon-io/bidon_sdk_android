package org.bidon.amazon.impl

import android.app.Activity
import org.bidon.amazon.SlotType
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.AdUnit

data class FullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotUuid: String? = adUnit.extra?.getString("slot_uuid")
    val format: SlotType? =
        adUnit.extra?.getString("format")?.let {
            SlotType.getOrNull(it).takeIf { it in arrayOf(SlotType.REWARDED_AD, SlotType.INTERSTITIAL, SlotType.VIDEO) }
        }
}