package org.bidon.yandex.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

class YandexBannerAuctionParam(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val lineItem: LineItem,
) : AdAuctionParams {
    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "UnityAdsBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$lineItem)"
    }
}