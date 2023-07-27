package org.bidon.sdk.auction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.auction.models.Bid
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.ext.SystemTimeNow

internal class ResultsCollectorImpl(
    private val resolver: AuctionResolver
) : ResultsCollector {
    /**
     * Keeps all succeeded auction results
     */
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())

    private val roundResult = MutableStateFlow<RoundResult>(RoundResult.Idle)
    override fun serverBiddingStarted() {
        roundResult.update {
            require(it is RoundResult.Results)
            RoundResult.Results(
                biddingResult = BiddingResult.ServerBiddingStarted(serverBiddingStartTs = SystemTimeNow),
                networkResults = it.networkResults,
                pricefloor = it.pricefloor,
                round = it.round
            )
        }
    }

    override fun serverBiddingFinished(bids: List<Bid>?) {
        roundResult.update { curRoundResult ->
            require(curRoundResult is RoundResult.Results)
            RoundResult.Results(
                biddingResult = run {
                    require(curRoundResult.biddingResult is BiddingResult.ServerBiddingStarted)
                    if (bids == null) {
                        BiddingResult.NoBid(
                            serverBiddingStartTs = curRoundResult.biddingResult.serverBiddingStartTs,
                            serverBiddingFinishTs = SystemTimeNow,
                        )
                    } else {
                        BiddingResult.FilledAd(
                            serverBiddingStartTs = curRoundResult.biddingResult.serverBiddingStartTs,
                            serverBiddingFinishTs = SystemTimeNow,
                            bids = bids,
                            results = emptyList()
                        )
                    }
                },
                networkResults = curRoundResult.networkResults,
                pricefloor = curRoundResult.pricefloor,
                round = curRoundResult.round
            )
        }
    }

    override fun startRound(round: Round, pricefloor: Double) {
        roundResult.value = RoundResult.Results(
            biddingResult = BiddingResult.ServerBiddingStarted(serverBiddingStartTs = SystemTimeNow),
            networkResults = emptyList(),
            pricefloor = pricefloor,
            round = round
        )
    }

    override fun addNetworkResult(networkResult: AuctionResult.Network) {
        roundResult.update {
            require(it is RoundResult.Results)
            RoundResult.Results(
                biddingResult = it.biddingResult,
                networkResults = it.networkResults + networkResult,
                pricefloor = it.pricefloor,
                round = it.round
            )
        }
    }

    override fun addBiddingResult(biddingResult: AuctionResult.Bidding) {
        roundResult.update {
            require(it is RoundResult.Results)
            RoundResult.Results(
                biddingResult = when (it.biddingResult) {
                    is BiddingResult.FilledAd -> {
                        BiddingResult.FilledAd(
                            serverBiddingStartTs = it.biddingResult.serverBiddingStartTs,
                            serverBiddingFinishTs = it.biddingResult.serverBiddingFinishTs,
                            bids = it.biddingResult.bids,
                            results = it.biddingResult.results + biddingResult
                        )
                    }

                    BiddingResult.Idle,
                    is BiddingResult.NoBid,
                    is BiddingResult.ServerBiddingStarted,
                    is BiddingResult.TimeoutReached -> {
                        it.biddingResult
                    }
                },
                networkResults = it.networkResults,
                pricefloor = it.pricefloor,
                round = it.round
            )
        }
    }

    override fun getAll(): List<AuctionResult> {
        return auctionResults.value
    }

    override fun clear() {
        auctionResults.value = emptyList()
        clearRoundResults()
    }

    override suspend fun saveWinners(sourcePriceFloor: Double) {
        val roundResults = when (val r = roundResult.value) {
            RoundResult.Idle -> emptyList()
            is RoundResult.Results -> (r.networkResults + (r.biddingResult as? BiddingResult.FilledAd)?.results.orEmpty())
        }
        val successfulResults = roundResults
            .filter { it.roundStatus == RoundStatus.Successful }
            .filter {
                /**
                 * Received ecpm should not be less then initial one [sourcePriceFloor].
                 */
                val isAbovePricefloor = it.adSource.getStats().ecpm >= sourcePriceFloor
                if (!isAbovePricefloor) {
                    (it.adSource as StatisticsCollector).markBelowPricefloor()
                }
                isAbovePricefloor
            }
        auctionResults.update {
            resolver
                .sortWinners(it + successfulResults)
                .also { list ->
                    val winner = list.getOrNull(0) ?: return
                    list.drop(ResultsCollector.MaxAuctionResultsAmount)
                        .forEach { auctionResult ->
                            val adSource = auctionResult.adSource
                            /**
                             *  Bidding demands should not be notified (server notifies them).
                             */
                            if (auctionResult !is AuctionResult.Bidding && adSource is WinLossNotifiable) {
                                logInfo(TAG, "Notified loss: ${adSource.demandId}")
                                adSource.notifyLoss(winner.adSource.demandId.demandId, winner.adSource.getStats().ecpm)
                            }
                            if (auctionResult.roundStatus == RoundStatus.Successful) {
                                adSource.markLoss()
                            }
                            logInfo(TAG, "Destroying loser: ${adSource.demandId}")
                            auctionResult.adSource.destroy()
                        }
                }
                .take(ResultsCollector.MaxAuctionResultsAmount)
        }
    }

    override fun biddingTimeoutReached(timeoutMs: Long) {
        roundResult.update {
            require(it is RoundResult.Results)
            val startTs = when (it.biddingResult) {
                is BiddingResult.ServerBiddingStarted -> it.biddingResult.serverBiddingStartTs
                is BiddingResult.FilledAd -> it.biddingResult.serverBiddingStartTs
                BiddingResult.Idle -> null
                is BiddingResult.NoBid -> it.biddingResult.serverBiddingStartTs
                is BiddingResult.TimeoutReached -> it.biddingResult.serverBiddingStartTs
            }
            RoundResult.Results(
                biddingResult = BiddingResult.TimeoutReached(serverBiddingStartTs = startTs ?: 0),
                networkResults = it.networkResults,
                pricefloor = it.pricefloor,
                round = it.round
            )
        }
    }

    override fun getRoundResults(): RoundResult = roundResult.value

    override fun clearRoundResults() {
        roundResult.value = RoundResult.Idle
    }
}

private const val TAG = "ResultsCollector"
