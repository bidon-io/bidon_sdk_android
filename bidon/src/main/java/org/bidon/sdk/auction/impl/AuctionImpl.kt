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
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.AuctionState
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.models.ExecuteRoundUseCase
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.SdkDispatchers
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
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())
    private val mutableLineItems = mutableListOf<LineItem>()
    private var _auctionDataResponse: AuctionResponse? = null
    private var _demandAd: DemandAd? = null
    private var job: Job? = null
    private val auctionDataResponse: AuctionResponse
        get() = requireNotNull(_auctionDataResponse)
    private var adTypeParam: AdTypeParam? = null

    override fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
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
                            resolver = resolver,
                            demandAd = demandAd,
                            adTypeParamData = adTypeParamData,
                        )
                        logInfo(Tag, "Rounds completed")

                        // Finding winner / notifying losers
                        val finalResults = auctionResults.value
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
                    val results = auctionResults.value.toList()
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
        auctionResults.value = emptyList()
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
                    adSource.notifyWin()
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
        resolver: AuctionResolver,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
    ) {
        val round = rounds.firstOrNull() ?: return

        // Execute round
        val allRoundResults = executeRound(
            round = round,
            pricefloor = pricefloor,
            demandAd = demandAd,
            adTypeParam = adTypeParamData,
            auctionResponse = auctionDataResponse,
            lineItems = mutableLineItems,
            onFinish = { remainingLineItems ->
                mutableLineItems.clear()
                mutableLineItems.addAll(remainingLineItems)
            }
        ).getOrNull() ?: emptyList()

        // Save round results
        proceedRoundResults(
            resolver = resolver,
            allResults = allRoundResults,
            sourcePriceFloor = sourcePriceFloor,
            round = round,
            pricefloor = pricefloor,
        )

        // Start next round
        val nextPriceFloor = auctionResults.value.firstOrNull()?.ecpm ?: pricefloor
        conductRounds(
            rounds = rounds.drop(1),
            sourcePriceFloor = sourcePriceFloor,
            pricefloor = nextPriceFloor,
            resolver = resolver,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
        )
    }

    private suspend fun proceedRoundResults(
        resolver: AuctionResolver,
        allResults: List<AuctionResult>,
        sourcePriceFloor: Double,
        round: Round,
        pricefloor: Double,
    ) {
        val sortedResult = resolver.sortWinners(allResults)
        val successfulResults = sortedResult
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

        /**
         * Save statistic data for /stats
         */
        auctionStat.addRoundResults(
            round = round,
            pricefloor = pricefloor,
            roundResults = sortedResult,
        )

        /**
         * Save auction results data
         */
        if (successfulResults.isNotEmpty()) {
            saveAuctionResults(
                resolver = resolver,
                roundResults = successfulResults
            )
        } else {
            logError(Tag, "Round '${round.id}' failed", BidonError.NoRoundResults)
        }
    }

    private suspend fun saveAuctionResults(
        resolver: AuctionResolver,
        roundResults: List<AuctionResult>
    ) {
        auctionResults.value = resolver.sortWinners(auctionResults.value + roundResults).take(2)
    }
}

private const val Tag = "Auction"
