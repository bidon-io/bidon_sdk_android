package org.bidon.sdk.auction.models

/**
 * Created by Bidon Team on 06/02/2023.
 */
@Deprecated("")
data class LineItem(
    val uid: String?,
    val demandId: String?,
    val pricefloor: Double = 0.0,
    val adUnitId: String?,
)
