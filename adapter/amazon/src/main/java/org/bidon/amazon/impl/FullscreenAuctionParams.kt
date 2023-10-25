package org.bidon.amazon.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.LineItem

data class FullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit,
    override val price: Double
) : AdAuctionParams {
    val slotUuid: String = requireNotNull(adUnit.extra?.getString("slotUuid")) {
        "slotUuid is null"
    }
}