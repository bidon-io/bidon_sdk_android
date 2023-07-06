package org.bidon.mobilefuse

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams

class MobileFuseAuctionParams(
    val activity: Activity,
    val payload: String,
) : AdAuctionParams {
    override val adUnitId: String? = null
    override val pricefloor: Double = 0.0
}
