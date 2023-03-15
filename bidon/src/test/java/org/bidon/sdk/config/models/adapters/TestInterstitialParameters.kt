package org.bidon.sdk.config.models.adapters

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

internal data class TestInterstitialParameters(
    val lineItem: LineItem
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
}