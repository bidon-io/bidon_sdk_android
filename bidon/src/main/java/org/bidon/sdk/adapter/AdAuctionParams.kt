package org.bidon.sdk.adapter

import android.app.Activity
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.ext.mapFailure

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdAuctionParams {
    val adUnit: AdUnit

    /**
     * DSP line item eCPM or Bidding bid price
     */
    val price: Double
}

class AdAuctionParamSource(
    val activity: Activity,
    /**
     * DSP pricefloor or Bidding bid price
     */
    val pricefloor: Double,
    val timeout: Long,
    private val adUnits: List<AdUnit> = emptyList(),
    private val onAdUnitsConsumed: (AdUnit) -> Unit = {},
    val bidResponse: BidResponse? = null,

    /**
     * Banner specific params
     */
    private val optBannerFormat: BannerFormat?,
    private val optContainerWidth: Float?,
) {
    val bannerFormat: BannerFormat get() = requireNotNull(optBannerFormat)
    val containerWidth: Float get() = requireNotNull(optContainerWidth)
    val requiredBidResponse: BidResponse get() = requireNotNull(bidResponse)

    operator fun <T> invoke(data: AdAuctionParamSource.() -> T): Result<T> = runCatching {
        data.invoke(this)
    }.mapFailure {
        BidonError.NoAppropriateAdUnitId
    }

    /**
     * Search for a [AdUnit] for the given demandId with the lowest pricefloor.
     * If the pricefloor exists, it will be consumed.
     */
    fun popAdUnit(demandId: DemandId, bidType: BidType): AdUnit? = adUnits
        .filter { it.bidType == bidType }
        .minByPricefloorOrNull(demandId, pricefloor)
        ?.also {
            if (it.pricefloor != null) {
                onAdUnitsConsumed(it)
            }
        }

    private fun List<AdUnit>.minByPricefloorOrNull(demandId: DemandId, pricefloor: Double): AdUnit? {
        return this
            .filter { it.demandId == demandId.demandId }
            .sortedBy { it.pricefloor }
            .firstOrNull { it.pricefloor?.let { it > pricefloor } ?: true }
    }
}
