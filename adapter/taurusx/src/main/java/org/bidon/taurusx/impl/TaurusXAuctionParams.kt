package org.bidon.taurusx.impl

import android.app.Activity
import android.content.Context
import com.taurusx.tax.core.AdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class TaurusXFullscreenAuctionParams(
    val context: Context,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload")
}

internal class TaurusXBannerAuctionParams(
    val activity: Activity,
    private val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.getString("placement_id")
    val payload: String? = adUnit.extra?.optString("payload")
    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> AdSize.Banner_300_250
            else -> AdSize.Banner_320_50
        }
}