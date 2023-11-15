package org.bidon.meta.impl

import android.app.Activity
import android.content.Context
import com.facebook.ads.AdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

class MetaFullscreenAuctionParams(
    val context: Context,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price = bidResponse.price
    val placementId = requireNotNull(bidResponse.adUnit.extra?.optString("placement_id")) {
        "Placement id is required for Meta"
    }
    val payload = requireNotNull(bidResponse.extra?.optString("payload")) {
        "Payload is required for Meta"
    }
}

class MetaBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price = bidResponse.price
    val placementId = requireNotNull(bidResponse.adUnit.extra?.optString("placement_id")) {
        "Placement id is required for Meta"
    }
    val payload = requireNotNull(bidResponse.extra?.optString("payload")) {
        "Payload is required for Meta"
    }

    val bannerSize: AdSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER_320_50
            BannerFormat.LeaderBoard -> AdSize.BANNER_HEIGHT_90
            BannerFormat.MRec -> AdSize.RECTANGLE_HEIGHT_250
            BannerFormat.Adaptive -> if (isTablet) AdSize.BANNER_HEIGHT_90 else AdSize.BANNER_HEIGHT_50
        }
}