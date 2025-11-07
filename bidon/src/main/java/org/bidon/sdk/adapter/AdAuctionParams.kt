package org.bidon.sdk.adapter

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.ext.mapFailure

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
public interface AdAuctionParams {
    public val adUnit: AdUnit

    /**
     * DSP line item eCPM or Bidding bid price
     */
    public val price: Double
}

public class AdAuctionParamSource(
    public val activity: Activity,
    /**
     * DSP pricefloor or Bidding bid price
     */
    public val pricefloor: Double,
    public val adUnit: AdUnit,

    /**
     * Banner specific params
     */
    private val optBannerFormat: BannerFormat?,
    private val optContainerWidth: Float?,
) {
    public val bannerFormat: BannerFormat get() = requireNotNull(optBannerFormat)
    public val containerWidth: Float get() = requireNotNull(optContainerWidth)

    public operator fun <T> invoke(data: AdAuctionParamSource.() -> T): Result<T> = runCatching {
        data.invoke(this)
    }.mapFailure {
        logError("AdAuctionParamSource", "${it?.message}", it)
        BidonError.NoAppropriateAdUnitId
    }
}
