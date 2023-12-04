package org.bidon.vungle

import android.app.Activity
import com.vungle.ads.BannerAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

class VungleBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload")) {
        "No payload found in bid response"
    }
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id")) {
        "placement_id is required"
    }

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> BannerAdSize.VUNGLE_MREC
            BannerFormat.LeaderBoard -> BannerAdSize.BANNER_LEADERBOARD
            BannerFormat.Banner -> BannerAdSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) BannerAdSize.BANNER_LEADERBOARD else BannerAdSize.BANNER
        }
}

class VungleFullscreenAuctionParams(
    val activity: Activity,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload")) {
        "No payload found in bid response"
    }
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id")) {
        "placement_id is required"
    }
}
