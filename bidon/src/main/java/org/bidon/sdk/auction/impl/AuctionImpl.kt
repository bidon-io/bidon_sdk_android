package org.bidon.sdk.auction.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.AuctionState
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.models.ExecuteRoundUseCase
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import java.util.UUID

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AuctionImpl(
    private val adaptersSource: AdaptersSource,
    private val getAuctionRequest: GetAuctionRequestUseCase,
    private val executeRound: ExecuteRoundUseCase,
    private val auctionStat: AuctionStat
) : Auction {
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val state = MutableStateFlow(AuctionState.Initialized)
    private val mutableLineItems = mutableListOf<LineItem>()
    private var _auctionDataResponse: AuctionResponse? = null
    private var _demandAd: DemandAd? = null
    private var job: Job? = null
    private val auctionDataResponse: AuctionResponse
        get() = requireNotNull(_auctionDataResponse)
    private var adTypeParam: AdTypeParam? = null
    private val resultsCollector: ResultsCollector by lazy { get() }

    override fun start(
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
        onSuccess: (results: List<AuctionResult>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        runCatching {
            if (state.compareAndSet(
                    expect = AuctionState.Initialized,
                    update = AuctionState.InProgress
                )
            ) {
                require(job?.isActive != true) {
                    "Auction is active"
                }
                this.adTypeParam = adTypeParamData
                job = scope.launch {
                    val auctionId = UUID.randomUUID().toString()
                    logInfo(Tag, "Action started $this")
                    // Request for Auction-data at /auction
                    auctionStat.markAuctionStarted(auctionId)
                    getAuctionRequest.request(
                        additionalData = adTypeParamData,
                        auctionId = auctionId,
                        demandAd = demandAd,
                        adapters = adaptersSource.adapters.associate {
                            it.demandId.demandId to it.adapterInfo
                        }
                    ).onSuccess { auctionData ->
//                        require(auctionId == auctionData.auctionId) {
//                            "auction_id has been changed"
//                        }
                        _auctionDataResponse = auctionData
                        _demandAd = demandAd
                        mutableLineItems.addAll(auctionData.lineItems ?: emptyList())

                        // Start auction
                        conductRounds(
                            rounds = auctionData.rounds ?: listOf(),
                            sourcePriceFloor = auctionData.pricefloor ?: 0.0,
                            pricefloor = auctionData.pricefloor ?: 0.0,
                            demandAd = demandAd,
                            adTypeParamData = adTypeParamData,
                        )
                        logInfo(Tag, "Rounds completed")

                        // Finding winner / notifying losers
                        val finalResults = resultsCollector.getAll()
                        logInfo(Tag, "Action finished with ${finalResults.size} results")
                        finalResults.forEachIndexed { index, auctionResult ->
                            logInfo(Tag, "Action result #$index: $auctionResult")
                        }
                        notifyWinLoss(finalResults)

                        // Sending auction statistics
                        auctionStat.sendAuctionStats(
                            auctionData = auctionData,
                            demandAd = demandAd,
                        )

                        // Finish auction
                        state.value = AuctionState.Finished
                    }.getOrThrow()
                    // Wait for auction is completed
                    state.first { it == AuctionState.Finished }
                    val results = resultsCollector.getAll()
                    clearData()
                    if (results.isNotEmpty()) {
                        onSuccess.invoke(results)
                    } else {
                        onFailure(BidonError.NoAuctionResults)
                    }
                }
            }
        }.onFailure(onFailure)
    }

    override fun cancel() {
        if (job?.isActive == true) {
            job?.cancel()
            auctionStat.markAuctionCanceled()
            proceedRoundResults()
            val auctionData = _auctionDataResponse
            if (auctionData == null) {
                logInfo(Tag, "No AuctionResponse info. There is nothing to send.")
            } else {
                auctionStat.sendAuctionStats(
                    auctionData = auctionData,
                    demandAd = requireNotNull(_demandAd),
                )
            }
            logInfo(Tag, "Auction canceled")
        }
        job = null
        clearData()
    }

    private fun clearData() {
        resultsCollector.clear()
        mutableLineItems.clear()
        _auctionDataResponse = null
    }

    private fun notifyWinLoss(finalResults: List<AuctionResult>) {
        val winner = finalResults.getOrNull(0) ?: return
        winner.adSource.markWin()
        finalResults.drop(1)
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
                adSource.destroy()
            }
    }

    private suspend fun conductRounds(
        rounds: List<Round>,
        sourcePriceFloor: Double,
        pricefloor: Double,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
    ) {
        val round = rounds.firstOrNull() ?: return
        resultsCollector.startRound(round, pricefloor)
        // Execute round
        executeRound(
            round = round,
            pricefloor = pricefloor,
            demandAd = demandAd,
            adTypeParam = adTypeParamData,
            auctionResponse = auctionDataResponse,
            lineItems = mutableLineItems,
            resultsCollector = resultsCollector,
            onFinish = { remainingLineItems ->
                mutableLineItems.clear()
                mutableLineItems.addAll(remainingLineItems)
            }
        )

        // Save round results
        resultsCollector.saveWinners(sourcePriceFloor)
        proceedRoundResults()

        // Start next round
        val nextPriceFloor = resultsCollector.getAll().firstOrNull()?.ecpm ?: pricefloor
        conductRounds(
            rounds = rounds.drop(1),
            sourcePriceFloor = sourcePriceFloor,
            pricefloor = nextPriceFloor,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
        )
    }

    private fun proceedRoundResults() {
        val (round, pricefloor, roundResults) = resultsCollector.popRoundResults() ?: return
        auctionStat.addRoundResults(
            round = round,
            pricefloor = pricefloor,
            roundResults = roundResults,
        )
        if (roundResults.isEmpty()) {
            logError(Tag, "Round '${round.id}' failed", BidonError.NoRoundResults)
        }
    }
}

private const val Tag = "Auction"
