package org.bidon.sdk.auction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.auction.ResultsCollector.Companion.MaxAuctionResultsAmount
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 05/07/2023.
 */
internal interface ResultsCollector {
    fun startRound(round: Round, pricefloor: Double)
    fun addAuctionResult(auctionResult: AuctionResult)
    fun getAll(): List<AuctionResult>
    fun clear()
    suspend fun saveWinners(sourcePriceFloor: Double)
    fun popRoundResults(): Triple<Round, Double, List<AuctionResult>>?

    companion object {
        /**
         * How many succeeded result to hold
         */
        const val MaxAuctionResultsAmount = 2
    }
}

internal class ResultsCollectorImpl(
    private val resolver: AuctionResolver
) : ResultsCollector {
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())
    private val lastRoundResults = MutableStateFlow(listOf<AuctionResult>())
    private var round: Round? = null
    private var roundPricefloor: Double = 0.0

    override fun startRound(round: Round, pricefloor: Double) {
        lastRoundResults.value = emptyList()
        this.round = round
        this.roundPricefloor = pricefloor
    }

    override fun addAuctionResult(auctionResult: AuctionResult) {
        lastRoundResults.update {
            it + auctionResult
        }
    }

    override fun getAll(): List<AuctionResult> {
        return auctionResults.value
    }

    override fun clear() {
        auctionResults.value = emptyList()
        lastRoundResults.value = emptyList()
    }

    override suspend fun saveWinners(sourcePriceFloor: Double) {
        lastRoundResults.update {
            resolver.sortWinners(it)
        }
        val successfulResults = lastRoundResults.value
            .filter { it.roundStatus == RoundStatus.Successful }
            .filter {
                /**
                 * Received ecpm should not be less then initial one [sourcePriceFloor].
                 */
                val isAbovePricefloor = it.ecpm >= sourcePriceFloor
                if (!isAbovePricefloor) {
                    (it.adSource as StatisticsCollector).markBelowPricefloor()
                }
                isAbovePricefloor
            }
        auctionResults.value = resolver
            .sortWinners(auctionResults.value + successfulResults)
            .also { list ->
                val winner = list.getOrNull(0) ?: return
                list.drop(MaxAuctionResultsAmount)
                    .forEach { auctionResult ->
                        val adSource = auctionResult.adSource
                        /**
                         *  Bidding demands should not be notified.
                         */
                        if (auctionResult !is AuctionResult.Bidding && adSource is WinLossNotifiable) {
                            logInfo(Tag, "Notified loss: ${adSource.demandId}")
                            adSource.notifyLoss(winner.adSource.demandId.demandId, winner.ecpm)
                        }
                        if (auctionResult.roundStatus == RoundStatus.Successful) {
                            adSource.markLoss()
                        }
                        logInfo(Tag, "Destroying loser: ${adSource.demandId}")
                        auctionResult.adSource.destroy()
                    }
            }
            .take(MaxAuctionResultsAmount)
    }

    override fun popRoundResults(): Triple<Round, Double, List<AuctionResult>>? {
        val result = round?.let {
            Triple(it, roundPricefloor, lastRoundResults.getAndUpdate { emptyList() })
        }
        round = null
        roundPricefloor = 0.0
        return result
    }
}

private const val Tag = "ResultsCollector"