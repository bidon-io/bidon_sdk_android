package org.bidon.sdk.adapter

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.utils.ext.mapFailure
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdAuctionParams {
    val adUnitId: String?
    val pricefloor: Double
}

class AdAuctionParamSource(
    val activity: Activity,
    val pricefloor: Double,
    val timeout: Long,
    private val lineItems: List<LineItem> = emptyList(),
    private val onLineItemConsumed: (LineItem) -> Unit = {},

    /**
     * Bid specific params
     */
    val json: JSONObject? = null,

    /**
     * Banner specific params
     */
    private val optBannerFormat: BannerFormat?,
    private val optContainerWidth: Float?,
) {
    val bannerFormat: BannerFormat get() = requireNotNull(optBannerFormat)
    val containerWidth: Float get() = requireNotNull(optContainerWidth)

    operator fun <T> invoke(data: AdAuctionParamSource.() -> T): Result<T> = runCatching {
        data.invoke(this)
    }.mapFailure {
        BidonError.NoAppropriateAdUnitId
    }

    /**
     * Search for a [LineItem] for the given demandId with the lowest pricefloor
     */
    fun popLineItem(demandId: DemandId): LineItem? = lineItems
        .minByPricefloorOrNull(demandId, pricefloor)
        ?.also(onLineItemConsumed)

    private fun List<LineItem>.minByPricefloorOrNull(demandId: DemandId, pricefloor: Double): LineItem? {
        return this
            .filter { it.demandId == demandId.demandId }
            .filterNot { it.adUnitId.isNullOrBlank() }
            .sortedBy { it.pricefloor }
            .firstOrNull { it.pricefloor > pricefloor }
    }
}
