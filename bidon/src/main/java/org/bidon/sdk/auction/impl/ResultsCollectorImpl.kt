package org.bidon.sdk.auction.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.AuctionResult.UnknownAdapter.Type
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
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

    override fun serverBiddingFinished(bids: List<BidResponse>?) {
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

    override fun startRound(round: RoundRequest, pricefloor: Double) {
        roundResult.value = RoundResult.Results(
            biddingResult = BiddingResult.ServerBiddingStarted(serverBiddingStartTs = SystemTimeNow),
            networkResults = emptyList(),
            pricefloor = pricefloor,
            round = round
        )
    }

    override fun add(result: AuctionResult) {
        roundResult.update {
            require(it is RoundResult.Results)
            when {
                result is AuctionResult.Bidding || (result as? AuctionResult.UnknownAdapter)?.type == Type.Bidding -> {
                    RoundResult.Results(
                        biddingResult = when (it.biddingResult) {
                            is BiddingResult.FilledAd -> {
                                BiddingResult.FilledAd(
                                    serverBiddingStartTs = it.biddingResult.serverBiddingStartTs,
                                    serverBiddingFinishTs = it.biddingResult.serverBiddingFinishTs,
                                    bids = it.biddingResult.bids,
                                    results = it.biddingResult.results + result
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

                result is AuctionResult.Network || (result as? AuctionResult.UnknownAdapter)?.type == Type.Network -> {
                    RoundResult.Results(
                        biddingResult = it.biddingResult,
                        networkResults = it.networkResults + result,
                        pricefloor = it.pricefloor,
                        round = it.round
                    )
                }

                else -> it
            }
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

    override fun biddingTimeoutReached() {
        roundResult.update {
            require(it is RoundResult.Results)
            val (startTs, finishTs) = when (it.biddingResult) {
                is BiddingResult.ServerBiddingStarted -> it.biddingResult.serverBiddingStartTs to null
                is BiddingResult.FilledAd -> it.biddingResult.serverBiddingStartTs to it.biddingResult.serverBiddingFinishTs
                BiddingResult.Idle -> null to null
                is BiddingResult.NoBid -> it.biddingResult.serverBiddingStartTs to it.biddingResult.serverBiddingFinishTs
                is BiddingResult.TimeoutReached -> it.biddingResult.serverBiddingStartTs to it.biddingResult.serverBiddingFinishTs
            }
            RoundResult.Results(
                biddingResult = BiddingResult.TimeoutReached(
                    serverBiddingStartTs = startTs ?: 0,
                    serverBiddingFinishTs = finishTs
                ),
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