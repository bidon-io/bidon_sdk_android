package org.bidon.dtexchange.impl

import android.app.Activity
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Bidon Team on 28/02/2023.
 */
data class DTExchangeAdAuctionParams(
    override val adUnit: AdUnit
) : AdAuctionParams {
    val spotId: String = requireNotNull(adUnit.extra?.optString("spot_id"))
    override val price: Double = requireNotNull(adUnit.pricefloor)
}

class DTExchangeBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit
) : AdAuctionParams {
    val spotId: String = requireNotNull(adUnit.extra?.optString("spot_id"))
    override val price: Double = requireNotNull(adUnit.pricefloor)

    override fun toString(): String {
        return "DTExchangeBannerAuctionParams(bannerFormat=$bannerFormat, lineItem=$adUnit)"
    }
}
