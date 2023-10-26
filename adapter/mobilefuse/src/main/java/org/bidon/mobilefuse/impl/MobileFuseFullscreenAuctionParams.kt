package org.bidon.mobilefuse.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 21/09/2023.
 */
class MobileFuseFullscreenAuctionParams(
    val activity: Activity,
    val signalData: String,
    val placementId: String,
    override val price: Double
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}

class MobileFuseBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    val signalData: String,
    val placementId: String,
    override val price: Double,
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}