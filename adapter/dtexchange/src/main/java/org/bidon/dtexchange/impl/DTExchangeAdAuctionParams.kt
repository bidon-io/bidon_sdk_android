package org.bidon.dtexchange.impl

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
data class DTExchangeAdAuctionParams(
    val lineItem: LineItem
) : AdAuctionParams {
    val spotId: String get() = requireNotNull(lineItem.adUnitId)
    override val adUnitId: String? get() = lineItem.adUnitId
}

data class DTExchangeBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val pricefloor: Double,
    val containerWidth: Float
) : AdAuctionParams {
    override val adUnitId: String get() = requireNotNull(lineItem.adUnitId)
}
