package org.bidon.sdk.adapter

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.utils.ext.mapFailure

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdAuctionParams {
    val adUnitId: String?
    val pricefloor: Double?
}

class AdAuctionParamSource(
    val activity: Activity,
    val pricefloor: Double,
    val timeout: Long,
    private val optBannerFormat: BannerFormat?,
    private val optContainerWidth: Float?,
    val lineItems: List<LineItem> = emptyList(),
    val payload: String? = null,
    val onLineItemConsumed: (LineItem) -> Unit = {},
) {
    val bannerFormat: BannerFormat get() = requireNotNull(optBannerFormat)
    val containerWidth: Float get() = requireNotNull(optContainerWidth)

    operator fun <T> invoke(data: AdAuctionParamSource.() -> T): Result<T> = runCatching {
        data.invoke(this)
    }.mapFailure {
        BidonError.NoAppropriateAdUnitId
    }
}
