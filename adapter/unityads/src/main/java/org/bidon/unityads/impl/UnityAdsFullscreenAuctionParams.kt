package org.bidon.unityads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
data class UnityAdsFullscreenAuctionParams(
    val lineItem: LineItem,
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val price: Double get() = lineItem.pricefloor
}

class UnityAdsBannerAuctionParams(
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val activity: Activity,
) : AdAuctionParams {
    override val adUnitId: String? get() = lineItem.adUnitId
    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "UnityAdsBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$lineItem)"
    }
}
