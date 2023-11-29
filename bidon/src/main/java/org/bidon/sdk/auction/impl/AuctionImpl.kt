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
import org.bidon.sdk.auction.Auction.AuctionState
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.models.RoundResult
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
    private val auctionStat: AuctionStat,
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
        adTypeParam: AdTypeParam,
        onSuccess: (results: List<AuctionResult>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (state.compareAndSet(
                expect = AuctionState.Initialized,
                update = AuctionState.InProgress
            )
        ) {
            if (job?.isActive == true) {
                logInfo(TAG, "Action in progress $this")
                return
            }
            this.adTypeParam = adTypeParam
            job = scope.launch {
                runCatching {
                    val auctionId = UUID.randomUUID().toString()
                    logInfo(TAG, "Action started $this")
                    // Request for Auction-data at /auction
                    auctionStat.markAuctionStarted(auctionId, adTypeParam)
                    getAuctionRequest.request(
                        adTypeParam = adTypeParam,
                        auctionId = auctionId,
                        demandAd = demandAd,
                        adapters = adaptersSource.adapters.associate {
                            it.demandId.demandId to it.adapterInfo
                        }
                    ).mapCatching { auctionData ->
                        if (auctionId != auctionData.auctionId) {
                            logError(TAG, "Auction ID has been changed", IllegalStateException())
                        }
                        conductAuction(
                            auctionData = auctionData,
                            demandAd = demandAd,
                            adTypeParamData = adTypeParam,
                        ).ifEmpty {
                            throw BidonError.NoAuctionResults
                        }.also {
                            adTypeParam.activity.runOnUiThread {
                                onSuccess(it)
                            }
                        }
                    }.onFailure {
                        logError(TAG, "Auction failed", it)
                        adTypeParam.activity.runOnUiThread {
                            onFailure(it)
                        }
                    }
                }.onFailure {
                    logError(TAG, "Auction failed", it)
                    adTypeParam.activity.runOnUiThread {
                        onFailure(it)
                    }
                }
            }
        }
    }

    override fun cancel() {
        if (job?.isActive == true) {
            job?.cancel()
            scope.launch {
                auctionStat.markAuctionCanceled()
                proceedRoundResults()
                val auctionData = _auctionDataResponse
                if (auctionData == null) {
                    logInfo(TAG, "No AuctionResponse info. There is nothing to send.")
                } else {
                    auctionStat.sendAuctionStats(
                        auctionData = auctionData,
                        demandAd = requireNotNull(_demandAd),
                    )
                }
                logInfo(TAG, "Auction canceled")
                clearData()
            }
        }
        job = null
    }

    private suspend fun conductAuction(
        auctionData: AuctionResponse,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
    ): List<AuctionResult> {
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
            roundIndex = 0,
        )
        logInfo(TAG, "Rounds completed")

        // Finding winner / notifying losers
        val finalResults = resultsCollector.getAll()
        logInfo(
            TAG,
            "Action finished with ${finalResults.size} results (keeps maximum: ${ResultsCollector.MaxAuctionResultsAmount})"
        )
        finalResults.forEachIndexed { index, auctionResult ->
            logInfo(TAG, "Action result #$index: $auctionResult")
        }

        // Sending auction statistics
        auctionStat.sendAuctionStats(
            auctionData = auctionData,
            demandAd = demandAd,
        )

        notifyWinLoss(finalResults)

        // Finish auction
        state.value = AuctionState.Finished
        // Wait for auction is completed
        state.first { it == AuctionState.Finished }
        val results = resultsCollector.getAll()
        clearData()
        return results
    }

    private suspend fun proceedRoundResults() {
        (resultsCollector.getRoundResults() as? RoundResult.Results)?.let {
            auctionStat.addRoundResults(it)
        }
        resultsCollector.clearRoundResults()
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
                    logInfo(TAG, "Notified loss: ${adSource.demandId}")
                    adSource.notifyLoss(winner.adSource.demandId.demandId, winner.adSource.getStats().ecpm)
                }
                if (auctionResult.roundStatus == RoundStatus.Successful) {
                    adSource.markLoss()
                }
                logInfo(TAG, "Destroying loser: ${adSource.demandId}")
                adSource.destroy()
            }
    }

    private suspend fun conductRounds(
        rounds: List<RoundRequest>,
        roundIndex: Int,
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
            roundIndex = roundIndex,
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
        val nextPriceFloor = resultsCollector.getAll().firstOrNull()?.adSource?.getStats()?.ecpm ?: pricefloor
        conductRounds(
            rounds = rounds.drop(1),
            sourcePriceFloor = sourcePriceFloor,
            pricefloor = nextPriceFloor,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
            roundIndex = roundIndex + 1,
        )
    }
}

private const val TAG = "Auction"
