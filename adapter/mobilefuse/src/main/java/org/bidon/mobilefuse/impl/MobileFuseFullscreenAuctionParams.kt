package org.bidon.mobilefuse.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Aleksei Cherniaev on 21/09/2023.
 */
class MobileFuseFullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val signalData: String? = adUnit.extra?.getString("signaldata")
    val placementId: String? = adUnit.extra?.getString("placement_id")
}

class MobileFuseBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val signalData: String? = adUnit.extra?.getString("signaldata")
    val placementId: String? = adUnit.extra?.getString("placement_id")
}