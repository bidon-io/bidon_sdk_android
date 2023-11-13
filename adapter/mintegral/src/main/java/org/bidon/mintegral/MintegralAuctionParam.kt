package org.bidon.mintegral

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 */
class MintegralAuctionParam(
    val activity: Activity,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))
    val unitId: String = requireNotNull(bidResponse.adUnit.extra?.getString("unit_id"))
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id"))

    override fun toString(): String {
        return "MintegralAuctionParam(price=$price, adUnitId=$adUnit, placementId=$placementId, payload='$payload')"
    }
}

class MintegralBannerAuctionParam(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    bidResponse: BidResponse,
) : AdAuctionParams {
    override val adUnit: AdUnit = bidResponse.adUnit
    override val price: Double = bidResponse.price
    val payload: String = requireNotNull(bidResponse.extra?.getString("payload"))
    val unitId: String = requireNotNull(bidResponse.adUnit.extra?.getString("unit_id"))
    val placementId: String = requireNotNull(bidResponse.adUnit.extra?.getString("placement_id"))
    override fun toString(): String {
        return "MintegralBannerAuctionParam($bannerFormat, price=$price, adUnitId=$adUnit, placementId=$placementId, payload='$payload')"
    }
}