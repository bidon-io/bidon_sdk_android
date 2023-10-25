package org.bidon.sdk.config.models.adapters

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.LineItem

internal data class TestInterstitialParameters(
    override val adUnit: AdUnit
) : AdAuctionParams {
    val adUnitId: String? get() = adUnit.extra?.getString("ad_unit_id")
    override val price: Double
        get() = requireNotNull(adUnit.pricefloor)
}