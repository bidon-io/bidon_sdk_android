package org.bidon.dtexchange.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Bidon Team on 28/02/2023.
 */
data class DTExchangeAdAuctionParams(
    val lineItem: LineItem
) : AdAuctionParams {
    val spotId: String get() = requireNotNull(lineItem.adUnitId)
    override val adUnitId: String? get() = lineItem.adUnitId
}