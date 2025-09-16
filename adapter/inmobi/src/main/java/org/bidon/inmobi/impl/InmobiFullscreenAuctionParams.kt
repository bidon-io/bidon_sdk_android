package org.bidon.inmobi.impl

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.AdUnit

class InmobiFullscreenAuctionParams(
    val context: Context,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val placementId: Long? = adUnit.extra?.optLong("placement_id")
    val payload: String? = adUnit.extra?.optString("payload")

    override fun toString(): String {
        return "InmobiFullscreenAuctionParams(placementId=$placementId, price=$price)"
    }
}
