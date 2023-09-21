package org.bidon.inmobi.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

class InmobiFullscreenAuctionParams(
    val activity: Activity,
    override val price: Double,
    override val lineItem: LineItem
) : AdAuctionParams {
    val placementId: Long = requireNotNull(lineItem.adUnitId).toLong()

    override fun toString(): String {
        return "InmobiFullscreenAuctionParams(placementId=$placementId, price=$price)"
    }
}
