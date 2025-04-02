package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit
import sg.bigo.ads.api.AdSize

internal class BigoAdsBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotId: String? = adUnit.extra?.getString("slot_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER
            BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
            BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
            BannerFormat.Adaptive -> if (isTablet) AdSize.LEADERBOARD else AdSize.BANNER
        }
}

internal class BigoAdsFullscreenAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val slotId: String? = adUnit.extra?.getString("slot_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM type
}
