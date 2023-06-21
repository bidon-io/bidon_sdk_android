package org.bidon.mintegral

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 */
data class MintegralAuctionParam(
    val activity: Activity,
    override val pricefloor: Double,
    val payload: String
) : AdAuctionParams {
    override val adUnitId: String? = null
}