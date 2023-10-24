package org.bidon.sdk.config.models.adapters

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

internal data class TestInterstitialParameters(
    override val adUnit: LineItem
) : AdAuctionParams {
    val adUnitId: String? get() = adUnit.adUnitId
    override val price: Double
        get() = adUnit.pricefloor
}