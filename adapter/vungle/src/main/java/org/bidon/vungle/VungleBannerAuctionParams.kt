package org.bidon.vungle

import android.app.Activity
import com.vungle.warren.AdConfig
import com.vungle.warren.BannerAdConfig
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

class VungleBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    bidResponse: BidResponse
) : AdAuctionParams {
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload")) {
        "Payload is required"
    }
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id")) {
        "placement_id is required"
    }
    override val adUnit: AdUnit = bidResponse.adUnit

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> AdConfig.AdSize.VUNGLE_MREC
            BannerFormat.LeaderBoard -> AdConfig.AdSize.BANNER_LEADERBOARD
            BannerFormat.Banner -> AdConfig.AdSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) AdConfig.AdSize.BANNER_LEADERBOARD else AdConfig.AdSize.BANNER
        }
    val config by lazy {
        BannerAdConfig().apply {
            this.adSize = bannerSize
        }
    }
}

data class VungleFullscreenAuctionParams(
    private val bidResponse: BidResponse
) : AdAuctionParams {
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload")) {
        "Payload is required"
    }
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id")) {
        "placement_id is required"
    }
    override val adUnit: AdUnit = bidResponse.adUnit
}
