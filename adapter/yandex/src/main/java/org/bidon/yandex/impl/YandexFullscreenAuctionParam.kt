package org.bidon.yandex.impl

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

class YandexFullscreenAuctionParam(
    val context: Context,
    override val lineItem: LineItem,
) : AdAuctionParams {
    override val price: Double get() = lineItem.pricefloor
    val adUnitId: String = requireNotNull(lineItem.adUnitId)
}