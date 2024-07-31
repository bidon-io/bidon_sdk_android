package org.bidon.mobilefuse.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

/**
 * Created by Aleksei Cherniaev on 21/09/2023.
 */
class MobileFuseFullscreenAuctionParams(
    val activity: Activity,
    bidResponse: BidResponse
) : AdAuctionParams {
    val signalData: String = requireNotNull(bidResponse.extra?.getString("signaldata"))
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id"))
    override val price: Double = bidResponse.price
    override val adUnit: AdUnit = bidResponse.adUnit
}

class MobileFuseBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    bidResponse: BidResponse
) : AdAuctionParams {
    val signalData: String = requireNotNull(bidResponse.extra?.getString("signaldata"))
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id"))
    override val price: Double = bidResponse.price
    override val adUnit: AdUnit = bidResponse.adUnit
}