package org.bidon.inmobi.impl

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

class InmobiFullscreenAuctionParams(
    val context: Context,
    override val price: Double,
    override val adUnit: LineItem
) : AdAuctionParams {
    val placementId: Long = requireNotNull(adUnit.adUnitId).toLong()

    override fun toString(): String {
        return "InmobiFullscreenAuctionParams(placementId=$placementId, price=$price)"
    }
}
