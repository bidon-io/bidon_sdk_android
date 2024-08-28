package org.bidon.mintegral

import android.app.Activity
import com.mbridge.msdk.out.BannerSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 */
internal class MintegralAuctionParam(
    val activity: Activity,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val unitId: String? = adUnit.extra?.getString("unit_id")
    val placementId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM mode

    override fun toString(): String {
        return "MintegralAuctionParam(price=$price, adUnitId=$adUnit, placementId=$placementId, payload='$payload')"
    }
}

internal class MintegralBannerAuctionParam(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val unitId: String? = adUnit.extra?.getString("unit_id")
    val placementId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload") // optional for CPM mode

    val bannerSize: BannerSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> BannerSize(BannerSize.STANDARD_TYPE, 320, 50)
            BannerFormat.LeaderBoard -> BannerSize(BannerSize.DEV_SET_TYPE, 728, 90)
            BannerFormat.MRec -> BannerSize(BannerSize.MEDIUM_TYPE, 300, 250)
            BannerFormat.Adaptive -> if (isTablet) {
                BannerSize(BannerSize.DEV_SET_TYPE, 728, 90)
            } else {
                BannerSize(BannerSize.STANDARD_TYPE, 320, 50)
            }
        }

    override fun toString(): String {
        return "MintegralBannerAuctionParam($bannerFormat, price=$price, adUnitId=$adUnit, placementId=$placementId, payload='$payload')"
    }
}