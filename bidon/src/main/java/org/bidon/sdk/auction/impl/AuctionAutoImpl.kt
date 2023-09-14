package org.bidon.sdk.auction.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.RoundManager
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.LineItemsPortal
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.util.UUID

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AuctionAutoImpl(
    private val adaptersSource: AdaptersSource,
    private val getAuctionRequest: GetAuctionRequestUseCase,
    private val executeRound: ExecuteRoundUseCase,
    private val auctionStat: AuctionStat,
    private val roundManager: RoundManager
) : Auction {
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val state = MutableStateFlow(Auction.AuctionState.Initialized)
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
        onFailure: (Throwable) -> Unit,
        onEach: (roundResults: List<AuctionResult>) -> Unit
    ) {
        if (state.compareAndSet(
                expect = Auction.AuctionState.Initialized,
                update = Auction.AuctionState.InProgress
            )
        ) {
            if (job?.isActive == true) {
                logInfo(TAG, "Action in progress $this")
                return
            }
            this.adTypeParam = adTypeParamData
            job = scope.launch {
                runCatching {
                    val auctionId = UUID.randomUUID().toString()
                    logInfo(TAG, "Action started $this")
                    // Request for Auction-data at /auction
                    auctionStat.markAuctionStarted(auctionId)
                    getAuctionRequest.request(
                        additionalData = adTypeParamData,
                        auctionId = auctionId,
                        demandAd = demandAd,
                        adapters = adaptersSource.adapters.associate {
                            it.demandId.demandId to it.adapterInfo
                        }
                    ).mapCatching { auctionData ->
                        if (!BidonSdk.bidon.isTestMode) {
                            check(auctionId == auctionData.auctionId) {
                                "auction_id has been changed"
                            }
                        }
                        roundManager.addLineItems(
                            when (demandAd.adType) {
                                AdType.Banner -> LineItemsPortal.bannerLineItems
                                AdType.Interstitial -> LineItemsPortal.interstitialLineItems
                                AdType.Rewarded -> error("Rewarded ads are not supported")
                            }
                        )
                        roundManager.setInitialPricefloor(newMinPricefloor = adTypeParamData.pricefloor)
                        conductAuction(
                            auctionData = auctionData,
                            demandAd = demandAd,
                            adTypeParamData = adTypeParamData,
                            onEach = onEach
                        ).ifEmpty {
                            throw BidonError.NoAuctionResults
                        }.also(onSuccess)
                    }.onFailure {
                        logError(TAG, "Auction failed", it)
                        onFailure(it)
                    }
                }.onFailure {
                    logError(TAG, "Auction failed", it)
                    onFailure(it)
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
        onEach: (roundResults: List<AuctionResult>) -> Unit,
    ): List<AuctionResult> {
        _auctionDataResponse = auctionData
        _demandAd = demandAd

        // Start auction
        conductRounds(
            pricefloor = adTypeParamData.pricefloor,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
            onEach = onEach
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
            auctionData = auctionData.copy(
                rounds = emptyList()
            ),
            demandAd = demandAd,
        )

        // Finish auction
        state.value = Auction.AuctionState.Finished
        clearData()
        return finalResults
    }

    private suspend fun proceedRoundResults() {
        val results = resultsCollector.getRoundResults()
        if (results is RoundResult.Results) {
            /**
             * Save round results to [AuctionStat]
             */
            auctionStat.addRoundResults(results)
        }

        resultsCollector.clearRoundResults()
    }

    private fun clearData() {
        resultsCollector.clear()
        _auctionDataResponse = null
    }

    private suspend fun conductRounds(
        pricefloor: Double,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
        onEach: (roundResults: List<AuctionResult>) -> Unit,
    ) {
        val nextRound = roundManager.popNextRound(pricefloor) ?: return
        resultsCollector.startRound(nextRound.roundRequest, pricefloor)
        logInfo(TAG, "Round started: ${nextRound.roundRequest}")
        logInfo(TAG, "Round started: ${nextRound.lineItems}")
        // Execute round
        executeRound(
            round = nextRound.roundRequest,
            pricefloor = pricefloor,
            demandAd = demandAd,
            adTypeParam = adTypeParamData,
            auctionResponse = auctionDataResponse,
            lineItems = nextRound.lineItems,
            resultsCollector = resultsCollector,
            roundIndex = nextRound.roundIndex,
            onFinish = { remainingLineItems ->
                if (remainingLineItems.isNotEmpty()) {
                    logError(TAG, "Remaining line items must not be: $remainingLineItems", IllegalStateException())
                }
            }
        )

        /**
         * Notify [RoundManager] about round results
         */
        val results = resultsCollector.getRoundResults()
        if (results is RoundResult.Results) {
            val allResults = (results.networkResults +
                    (results.biddingResult as? BiddingResult.FilledAd)?.results.orEmpty())
            val successfulResults = allResults.filter {
                it.roundStatus == RoundStatus.Successful
            }
            if (successfulResults.isEmpty()) {
                nextRound.lineItems.lastOrNull()?.pricefloor?.let {
                    roundManager.notifyFail(newMaxPricefloor = it)
                }
            } else {
                val newMinPricefloor = successfulResults.maxOfOrNull { it.adSource.getStats().ecpm }
                    ?: nextRound.lineItems.firstOrNull()?.pricefloor
                newMinPricefloor?.let {
                    roundManager.notifyLoaded(newMinPricefloor = it)
                }
                onEach.invoke(successfulResults)
            }
        }

        // Save round results
        resultsCollector.saveWinners(sourcePriceFloor = adTypeParamData.pricefloor)
        proceedRoundResults()

        // Start next round
        val nextPriceFloor = resultsCollector.getAll().firstOrNull()?.adSource?.getStats()?.ecpm ?: pricefloor
        conductRounds(
            pricefloor = nextPriceFloor,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
            onEach = onEach,
        )
    }
}