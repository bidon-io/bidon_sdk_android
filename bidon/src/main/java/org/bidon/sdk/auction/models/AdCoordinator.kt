package org.bidon.sdk.auction.models

import kotlinx.coroutines.flow.MutableStateFlow
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.BinarySearchRound
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.utils.di.get

internal interface AdCoordinator {
    /**
     * @param existing shows cached bids
     */
    fun startAuction(existing: Map<DemandId, BidStat>, pricefloor: Double)

    fun notifyFail(newMaxPricefloor: Double)
    fun notifyLoaded(newMinPricefloor: Double)

    fun popNextRound(pricefloor: Double): NextRound?

    class NextRound(
        val lineItems: List<AdItem>,
        val roundRequest: RoundRequest,
        val roundIndex: Int
    )
}

internal class AdCoordinatorImpl(
    private val allDspAdItems: List<AdItem>,
    private val allBiddingParticipants: List<String>
) : AdCoordinator {
    private var binarySearchRound: BinarySearchRound = get()
    private val stateFlow = MutableStateFlow(State.Idle)

    override fun startAuction(existing: Map<DemandId, BidStat>, pricefloor: Double) {
        binarySearchRound = get()
        stateFlow.value = State.Idle
        binarySearchRound.addLineItems(
            lineItems = allDspAdItems.filter { adItem ->
                adItem.lineItem.demandId !in existing.keys.map { it.demandId }
            }.map {
                it.lineItem
            },
            bidding = allBiddingParticipants.filter { demandId ->
                demandId !in existing.keys.map { it.demandId }
            },
            minPrice = pricefloor
        )
    }

    override fun notifyFail(newMaxPricefloor: Double) {
        binarySearchRound.notifyFail(newMaxPricefloor)
    }

    override fun notifyLoaded(newMinPricefloor: Double) {
        binarySearchRound.notifyLoaded(newMinPricefloor)
    }

    override fun popNextRound(pricefloor: Double): AdCoordinator.NextRound? {
        // efficient round
        if (stateFlow.value == State.Idle) {
            val efficientLineItems = getEfficientItems()
            if (efficientLineItems.isNotEmpty()) {
                return AdCoordinator.NextRound(
                    lineItems = efficientLineItems,
                    roundRequest = RoundRequest(
                        id = "ROUND-EFFICIENT",
                        timeoutMs = 15000,
                        demandIds = efficientLineItems.mapNotNull { it.lineItem.demandId },
                        biddingIds = emptyList(),
                    ),
                    roundIndex = 0
                )
            }
        }
        // binary-search round
        // bidding round

        return TODO()
    }

    private fun getEfficientItems(): List<AdItem> {
        return allDspAdItems.sortByWeights()
    }

    private fun List<AdItem>.sortByWeights(maxItems: Int = 4): List<AdItem> {
        return this
            .filter { it.weight() > 0.0 }
            .sortedByDescending { it.weight() }
            .take(maxItems)
    }

    enum class State {
        Idle,
        Efficient,
        BinarySearch,
        Bidding
    }


}