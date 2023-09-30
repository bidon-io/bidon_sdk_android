package org.bidon.sdk.auction.models

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.cache.Cacheable
import org.bidon.sdk.auction.BinarySearchRoundCoordinator
import org.bidon.sdk.auction.models.AdCoordinator.NextRound
import org.bidon.sdk.auction.models.AdCoordinator.RoundType
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

internal interface AdCoordinator {
    /**
     * @param existing shows cached bids
     */
    fun onAuctionStarted(existing: Map<DemandId, BidStat>, pricefloor: Double, settings: Cacheable.Settings)
    fun popNextRound(pricefloor: Double): NextRound?
    fun notifyRoundCompleted(results: RoundResult.Results, successfulResults: List<AuctionResult>)

    class NextRound(
        val adItems: List<AdItem>,
        val roundRequest: RoundRequest,
        val roundIndex: Int,
        val roundType: RoundType
    ) {
        override fun toString(): String {
            return "NextRound(#$roundIndex [${
            adItems.joinToString {
                "${it.lineItem.demandId}(${it.lineItem.pricefloor})"
            }
            }] $roundRequest)"
        }
    }

    enum class RoundType {
        IDLE,
        DSP_EFFICIENT,
        DSP_BINARY_SEARCH,
        BIDDING,
        FINISHED,
    }
}

internal class AdCoordinatorImpl(
    private val allDspAdItems: List<AdItem>,
    private val allBiddingParticipants: List<String>
) : AdCoordinator {
    private var binarySearchRoundCoordinator: BinarySearchRoundCoordinator = get()
    private var nextRound: NextRound? = null
    private var nextRoundType: RoundType = RoundType.IDLE

    private lateinit var settings: Cacheable.Settings

    override fun onAuctionStarted(existing: Map<DemandId, BidStat>, pricefloor: Double, settings: Cacheable.Settings) {
        this.settings = settings
        nextRoundType = RoundType.IDLE
        nextRound = null
        binarySearchRoundCoordinator = get<BinarySearchRoundCoordinator>().apply {
            addLineItems(
                adItems = allDspAdItems.filter { adItem ->
                    adItem.lineItem.demandId !in existing.keys.map { it.demandId }
                },
                minPrice = pricefloor
            )
        }
    }

    override fun popNextRound(pricefloor: Double): NextRound? {
        logInfo(TAG, "-> \$$pricefloor $nextRoundType")
        if (nextRoundType == RoundType.FINISHED) {
            nextRound = null
            return null
        }
        nextRound = createNextRound(pricefloor).also {
            logInfo(TAG, "<- $nextRoundType: $it")
        }
        return nextRound
    }

    override fun notifyRoundCompleted(results: RoundResult.Results, successfulResults: List<AuctionResult>) {
        /**
         * Saving local stats
         */
        results.networkResults.forEach { result ->
            allDspAdItems
                .find { it.lineItem.uid == result.adSource.getStats().lineItemUid }
                ?.loaded(result.roundStatus == RoundStatus.Successful)
        }
//        allDspAdItems.forEach {
//            logInfo(TAG, "$it")
//        }
        /**
         * Notify Binary search coordinator
         */
        if (nextRoundType == RoundType.DSP_BINARY_SEARCH) {
            val nextRound = nextRound ?: return
            if (successfulResults.isEmpty()) {
                if (nextRound.roundType != RoundType.DSP_EFFICIENT) {
                    nextRound.adItems.lastOrNull()?.lineItem?.pricefloor?.let {
                        binarySearchRoundCoordinator.notifyFail(newMaxPricefloor = it)
                    }
                }
            } else {
                val newMinPricefloor = successfulResults.maxOfOrNull { it.adSource.getStats().ecpm }
                    ?: nextRound.adItems.firstOrNull()?.lineItem?.pricefloor
                newMinPricefloor?.let {
                    binarySearchRoundCoordinator.notifyLoaded(newMinPricefloor = it)
                }
            }
        }
    }

    private fun createNextRound(pricefloor: Double): NextRound? {
        if (nextRoundType == RoundType.IDLE) {
            nextRoundType = if (settings.useEfficientRound) {
                RoundType.DSP_EFFICIENT
            } else {
                RoundType.DSP_BINARY_SEARCH
            }
        }

        if (nextRoundType == RoundType.DSP_EFFICIENT) {
            val efficientLineItems = getEfficientItems().filter {
                it.lineItem.pricefloor > pricefloor
            }
            nextRoundType = RoundType.DSP_BINARY_SEARCH
            if (efficientLineItems.isNotEmpty()) {
                return NextRound(
                    adItems = efficientLineItems,
                    roundRequest = RoundRequest(
                        id = "ROUND-EFFICIENT",
                        timeoutMs = 5000,
                        demandIds = efficientLineItems.mapNotNull { it.lineItem.demandId },
                        biddingIds = emptyList(),
                    ),
                    roundIndex = 0,
                    roundType = RoundType.DSP_EFFICIENT
                )
            }
        }

        if (nextRoundType == RoundType.DSP_BINARY_SEARCH) {
            val nextRound = binarySearchRoundCoordinator.popNextRound(pricefloor)
            if (nextRound != null) {
                return nextRound
            } else {
                nextRoundType = RoundType.BIDDING
            }
        }

        if (nextRoundType == RoundType.BIDDING) {
            nextRoundType = RoundType.FINISHED
            if (settings.useBiddingRound) {
                return NextRound(
                    adItems = emptyList(),
                    roundRequest = RoundRequest(
                        id = "ROUND-BIDDING",
                        timeoutMs = 10000,
                        demandIds = emptyList(),
                        biddingIds = allBiddingParticipants,
                    ),
                    roundIndex = (nextRound?.roundIndex ?: 0) + 1,
                    roundType = RoundType.BIDDING
                )
            }
        }
        return null
    }

    private fun getEfficientItems(): List<AdItem> {
        return allDspAdItems.sortByWeights().distinctBy {
            it.lineItem.demandId
        }
    }

    private fun List<AdItem>.sortByWeights(maxItems: Int = 4): List<AdItem> {
        return this
            .filter { it.weight() > 0.0 }
            .sortedByDescending { it.weight() }
            .take(maxItems)
    }
}