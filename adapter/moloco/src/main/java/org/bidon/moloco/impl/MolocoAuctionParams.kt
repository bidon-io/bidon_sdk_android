package org.bidon.moloco.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

internal class MolocoFullscreenAuctionParams(
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")
    val payload: String? = adUnit.extra?.optString("payload")
}

internal class MolocoBannerAuctionParams(
    private val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.getString("ad_unit_id")
    val payload: String? = adUnit.extra?.optString("payload")
    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec,
            BannerFormat.Banner,
            BannerFormat.LeaderBoard -> bannerFormat
            BannerFormat.Adaptive -> if (isTablet) BannerFormat.LeaderBoard else BannerFormat.Banner
        }
}
