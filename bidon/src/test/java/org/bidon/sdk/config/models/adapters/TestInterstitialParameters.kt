package org.bidon.sdk.config.models.adapters

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

internal data class TestInterstitialParameters(
    override val lineItem: LineItem
) : AdAuctionParams {
    val adUnitId: String? get() = lineItem.adUnitId
    override val price: Double
        get() = lineItem.pricefloor
}