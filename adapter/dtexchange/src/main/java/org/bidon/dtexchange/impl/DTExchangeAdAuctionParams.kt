package org.bidon.dtexchange.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal data class DTExchangeAdAuctionParams(
    override val adUnit: AdUnit
) : AdAuctionParams {
    val spotId: String? = adUnit.extra?.optString("spot_id")
    override val price: Double = adUnit.pricefloor
}

internal class DTExchangeBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    val spotId: String? = adUnit.extra?.optString("spot_id")
    override val price: Double = adUnit.pricefloor

    override fun toString(): String {
        return "DTExchangeBannerAuctionParams(bannerFormat=$bannerFormat, adUnit=$adUnit)"
    }
}
