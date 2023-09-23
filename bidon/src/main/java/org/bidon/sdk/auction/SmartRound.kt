package org.bidon.sdk.auction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.auction.SmartRound.AdaptiveRound
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG

/**
 * Created by Aleksei Cherniaev on 08/09/2023.
 */
internal interface SmartRound {
    fun addLineItems(lineItems: List<LineItem>, bidding: List<String>, minPrice: Double)
    fun notifyFail(newMaxPricefloor: Double)
    fun notifyLoaded(newMinPricefloor: Double)

    fun popNextRound(pricefloor: Double): NextRound?

    data class AdaptiveRound(
        val index: Int,
        val minPrice: Double,
        val maxPrice: Double,
        val lineItems: List<LineItem>,
        val bidding: List<String>,
    )

    data class NextRound(
        val lineItems: List<LineItem>,
        val roundRequest: RoundRequest,
        val roundIndex: Int
    )
}

internal class SmartRoundImpl : SmartRound {
    private val flow = MutableStateFlow(
        AdaptiveRound(
            index = 0,
            minPrice = 0.0,
            maxPrice = 0.0,
            lineItems = emptyList(),
            bidding = emptyList()
        )
    )

    override fun addLineItems(lineItems: List<LineItem>, bidding: List<String>, minPrice: Double) {
        logInfo(TAG, "Add line items. Count = ${lineItems.size}")
        logInfo(TAG, "Add line items. $lineItems")
        flow.update {
            AdaptiveRound(
                minPrice = minOf(minPrice, lineItems.minBy { it.pricefloor }.pricefloor - 0.001),
                maxPrice = lineItems.maxBy { it.pricefloor }.pricefloor - 0.001,
                lineItems = lineItems.filterNot {
                    it.pricefloor <= minPrice
                },
                bidding = bidding,
                index = 0,
            )
        }
    }

    override fun notifyFail(newMaxPricefloor: Double) {
        logInfo(TAG, "Round fail. NewMaxPricefloor = $newMaxPricefloor")
        flow.update { type ->
            (type as? AdaptiveRound)?.copy(
                maxPrice = newMaxPricefloor,
                lineItems = type.lineItems.filterNot {
                    it.pricefloor >= newMaxPricefloor
                }
            ) ?: type
        }
    }

    override fun notifyLoaded(newMinPricefloor: Double) {
        logInfo(TAG, "Round loaded. NewMinPricefloor = $newMinPricefloor")
        flow.update { type ->
            (type as? AdaptiveRound)?.copy(
                minPrice = newMinPricefloor,
                lineItems = type.lineItems.filterNot {
                    it.pricefloor <= newMinPricefloor
                }
            ) ?: type
        }
    }

    override fun popNextRound(pricefloor: Double): SmartRound.NextRound? {
        val adaptiveRound = flow.value
        val lineItems = adaptiveRound.lineItems.sortedBy { it.pricefloor }.ifEmpty {
            logInfo(TAG, "No line items. Rounds finished.")
            return if (flow.value.bidding.isNotEmpty()) {
                val bidding = flow.value.bidding
                flow.update { round ->
                    round.copy(
                        bidding = emptyList()
                    )
                }
                SmartRound.NextRound(
                    lineItems = emptyList(),
                    roundRequest = RoundRequest(
                        id = "ROUND-BIDDING",
                        timeoutMs = 15000,
                        demandIds = emptyList(),
                        biddingIds = bidding,
                    ),
                    roundIndex = flow.value.index
                )
            } else {
                /**
                 * All rounds finished
                 */
                null
            }
        }
        val middlePrice = lineItems[lineItems.size / 2].pricefloor - 0.001
        val demands = lineItems.groupBy { it.demandId }
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
                lineItems = round.lineItems - demands.map { it.second }.toSet(),
            )
        }
        return SmartRound.NextRound(
            lineItems = demands.map { it.second },
            roundRequest = RoundRequest(
                id = "ROUND-${flow.value.index}",
                timeoutMs = 15000,
                demandIds = demands.map { it.first },
                biddingIds = emptyList(),
            ),
            roundIndex = flow.value.index
        ).also {
            logInfo(TAG, "Next round: $it")
        }
    }

    private fun List<LineItem>.minByPricefloorOrNull(pricefloor: Double): LineItem? {
        return this
            .filterNot { it.adUnitId.isNullOrBlank() }
            .sortedBy { it.pricefloor }
            .firstOrNull { it.pricefloor > pricefloor }
    }
}