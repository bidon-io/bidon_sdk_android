package org.bidon.sdk.auction.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.AuctionResult.UnknownAdapter.Type
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
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

    @Deprecated("")
    override fun serverBiddingStarted() {
        roundResult.update {
            require(it is RoundResult.Results)
            RoundResult.Results(
                biddingResult = BiddingResult.ServerBiddingStarted(serverBiddingStartTs = SystemTimeNow),
                networkResults = it.networkResults,
                pricefloor = it.pricefloor,
            )
        }
    }

    @Deprecated("")
    override fun serverBiddingFinished(adUnits: List<AdUnit>?) {
        roundResult.update { curRoundResult ->
            when (curRoundResult) {
                RoundResult.Idle -> curRoundResult
                is RoundResult.Results -> {
                    RoundResult.Results(
                        biddingResult = run {
                            if (curRoundResult.biddingResult is BiddingResult.ServerBiddingStarted) {
                                if (adUnits.isNullOrEmpty()) {
                                    BiddingResult.NoBid(
                                        serverBiddingStartTs = curRoundResult.biddingResult.serverBiddingStartTs,
                                        serverBiddingFinishTs = SystemTimeNow,
                                    )
                                } else {
                                    BiddingResult.FilledAd(
                                        serverBiddingStartTs = curRoundResult.biddingResult.serverBiddingStartTs,
                                        serverBiddingFinishTs = SystemTimeNow,
                                        adUnits = adUnits,
                                        results = emptyList()
                                    )
                                }
                            } else {
                                logError(TAG, "Unexpected bidding result: ${curRoundResult.biddingResult}", null)
                                curRoundResult.biddingResult
                            }
                        },
                        networkResults = curRoundResult.networkResults,
                        pricefloor = curRoundResult.pricefloor,
                    )
                }
            }
        }
    }

    override fun startRound(pricefloor: Double) {
        roundResult.value = RoundResult.Results(
            biddingResult = BiddingResult.Idle,
            networkResults = emptyList(),
            pricefloor = pricefloor,
        )
    }

    override fun add(result: AuctionResult) {
        roundResult.update { current ->
            require(current is RoundResult.Results)
            when {
                result is AuctionResult.BiddingLose ||
                    result is AuctionResult.Bidding ||
                    (result as? AuctionResult.UnknownAdapter)?.type == Type.Bidding -> {
                    RoundResult.Results(
                        biddingResult = when (current.biddingResult) {
                            is BiddingResult.FilledAd -> {
                                BiddingResult.FilledAd(
                                    serverBiddingStartTs = current.biddingResult.serverBiddingStartTs,
                                    serverBiddingFinishTs = current.biddingResult.serverBiddingFinishTs,
                                    adUnits = current.biddingResult.adUnits,
                                    results = current.biddingResult.results + result
                                )
                            }

                            BiddingResult.Idle,
                            is BiddingResult.NoBid,
                            is BiddingResult.ServerBiddingStarted,
                            is BiddingResult.TimeoutReached -> {
                                current.biddingResult
                            }
                        },
                        networkResults = current.networkResults,
                        pricefloor = current.pricefloor,
                    )
                }

                result is AuctionResult.Network || (result as? AuctionResult.UnknownAdapter)?.type == Type.Network -> {
                    RoundResult.Results(
                        biddingResult = current.biddingResult,
                        networkResults = current.networkResults + result,
                        pricefloor = current.pricefloor,
                    )
                }

                else -> current
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

    @Deprecated("")
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
                    it.adSource.markBelowPricefloor()
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
                is BiddingResult.ServerBiddingStarted -> it.biddingResult.serverBiddingStartTs to SystemTimeNow
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
            )
        }
    }

    override fun getRoundResults(): RoundResult = roundResult.value

    override fun clearRoundResults() {
        roundResult.value = RoundResult.Idle
    }
}

private const val TAG = "ResultsCollector"
