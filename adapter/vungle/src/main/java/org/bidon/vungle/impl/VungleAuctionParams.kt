package org.bidon.vungle.impl

import android.app.Activity
import com.vungle.ads.VungleAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

internal class VungleBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val placementId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> VungleAdSize.MREC
            BannerFormat.LeaderBoard -> VungleAdSize.BANNER_LEADERBOARD
            BannerFormat.Banner -> VungleAdSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) VungleAdSize.BANNER_LEADERBOARD else VungleAdSize.BANNER
        }
}

internal class VungleFullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val placementId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type
}
