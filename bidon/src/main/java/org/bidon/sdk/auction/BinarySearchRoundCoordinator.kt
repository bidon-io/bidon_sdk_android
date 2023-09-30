package org.bidon.sdk.auction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.auction.BinarySearchRoundCoordinator.AdaptiveRound
import org.bidon.sdk.auction.models.AdCoordinator
import org.bidon.sdk.auction.models.AdCoordinator.NextRound
import org.bidon.sdk.auction.models.AdItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG

/**
 * Created by Aleksei Cherniaev on 08/09/2023.
 */
internal interface BinarySearchRoundCoordinator {
    fun addLineItems(adItems: List<AdItem>, minPrice: Double)
    fun notifyFail(newMaxPricefloor: Double)
    fun notifyLoaded(newMinPricefloor: Double)

    fun popNextRound(pricefloor: Double): NextRound?

    data class AdaptiveRound(
        val index: Int,
        val minPrice: Double,
        val maxPrice: Double,
        val adItems: List<AdItem>,
    )
}

internal class BinarySearchRoundCoordinatorImpl : BinarySearchRoundCoordinator {
    private val flow = MutableStateFlow(
        AdaptiveRound(
            index = 0,
            minPrice = 0.0,
            maxPrice = 0.0,
            adItems = emptyList(),
        )
    )

    override fun addLineItems(adItems: List<AdItem>, minPrice: Double) {
        logInfo(TAG, "Add line items. Count = ${adItems.size}")
        logInfo(TAG, "Add line items. $adItems")
        if (adItems.isEmpty()) {
            return
        }
        flow.update {
            AdaptiveRound(
                minPrice = minOf(minPrice, adItems.minBy { it.lineItem.pricefloor }.lineItem.pricefloor - 0.001),
                maxPrice = adItems.maxBy { it.lineItem.pricefloor }.lineItem.pricefloor - 0.001,
                adItems = adItems.filterNot {
                    it.lineItem.pricefloor <= minPrice
                },
                index = 0,
            )
        }
    }

    override fun notifyFail(newMaxPricefloor: Double) {
        logInfo(TAG, "Round fail. NewMaxPricefloor = $newMaxPricefloor")
        flow.update { type ->
            (type as? AdaptiveRound)?.copy(
                maxPrice = newMaxPricefloor,
                adItems = type.adItems.filterNot {
                    it.lineItem.pricefloor >= newMaxPricefloor
                }
            ) ?: type
        }
    }

    override fun notifyLoaded(newMinPricefloor: Double) {
        logInfo(TAG, "Round loaded. NewMinPricefloor = $newMinPricefloor")
        flow.update { type ->
            (type as? AdaptiveRound)?.copy(
                minPrice = newMinPricefloor,
                adItems = type.adItems.filterNot {
                    it.lineItem.pricefloor <= newMinPricefloor
                }
            ) ?: type
        }
    }

    override fun popNextRound(pricefloor: Double): NextRound? {
        val adaptiveRound = flow.value
        val lineItems = adaptiveRound.adItems.sortedBy { it.lineItem.pricefloor }.ifEmpty {
            logInfo(TAG, "No line items. Rounds finished.")
            return null
        }
        val middlePrice = lineItems[lineItems.size / 2].lineItem.pricefloor - 0.001
        val demands = lineItems.groupBy { it.lineItem.demandId }
            .mapNotNull { (demandId, lineItems) ->
                demandId ?: return null
                val minLineItem = lineItems.minByPricefloorOrNull(middlePrice) ?: return@mapNotNull null
                demandId to minLineItem
            }.ifEmpty {
                logInfo(TAG, "No demands with pricefloor > $middlePrice. Rounds finished.")
                return null
            }
        flow.update { round ->
            round.copy(
                index = round.index + 1,
                adItems = round.adItems - demands.map { it.second }.toSet(),
            )
        }
        return NextRound(
            adItems = demands.map { it.second },
            roundRequest = RoundRequest(
                id = "ROUND-${flow.value.index}",
                timeoutMs = 15000,
                demandIds = demands.map { it.first },
                biddingIds = emptyList(),
            ),
            roundIndex = flow.value.index,
            roundType = AdCoordinator.RoundType.DSP_BINARY_SEARCH
        ).also {
            logInfo(TAG, "Next round: $it")
        }
    }

    private fun List<AdItem>.minByPricefloorOrNull(pricefloor: Double): AdItem? {
        return this
            .filterNot { it.lineItem.adUnitId.isNullOrBlank() }
            .sortedBy { it.lineItem.pricefloor }
            .firstOrNull { it.lineItem.pricefloor > pricefloor }
    }
}