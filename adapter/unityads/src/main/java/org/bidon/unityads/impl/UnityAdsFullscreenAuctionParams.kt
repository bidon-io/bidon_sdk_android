package org.bidon.unityads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
data class UnityAdsFullscreenAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = requireNotNull(adUnit.pricefloor)
    val placementId: String = requireNotNull(adUnit.extra?.getString("placement_id"))
}

class UnityAdsBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = requireNotNull(adUnit.pricefloor)
    val placementId: String = requireNotNull(adUnit.extra?.getString("placement_id"))

    override fun toString(): String {
        return "UnityAdsBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$adUnit)"
    }
}
