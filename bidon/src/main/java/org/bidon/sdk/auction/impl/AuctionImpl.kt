package org.bidon.sdk.auction.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.ext.toAuctionInfo
import org.bidon.sdk.ads.ext.toAuctionNoBidInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.Auction.AuctionState
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.ext.printWaterfall
import org.bidon.sdk.auction.models.AuctionCancellation
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.TokenInfo
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.ExecuteAuctionUseCase
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.GetTokensUseCase
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.bidding.BiddingConfig
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStat
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
    private val executeAuction: ExecuteAuctionUseCase,
    private val auctionStat: AuctionStat,
    private val tokenGetter: GetTokensUseCase,
    private val biddingConfig: BiddingConfig,
) : Auction {
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val state = MutableStateFlow(AuctionState.Initialized)

    private var _auctionDataResponse: AuctionResponse? = null
    private var _demandAd: DemandAd? = null
    private var job: Job? = null
    private var adTypeParam: AdTypeParam? = null
    private val resultsCollector: ResultsCollector by lazy { get() }

    override fun start(
        demandAd: DemandAd,
        adTypeParam: AdTypeParam,
        onSuccess: (results: List<AuctionResult>, auctionInfo: AuctionInfo) -> Unit,
        onFailure: (AuctionInfo?, Throwable) -> Unit
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
                    logInfo(TAG, "Auction started $this")
                    resultsCollector.startRound(adTypeParam.pricefloor)
                    resultsCollector.serverBiddingStarted()

                    val tokens = tokenGetter.invoke(
                        adType = demandAd.adType,
                        adTypeParam = adTypeParam,
                        adaptersSource = adaptersSource,
                        tokenTimeout = biddingConfig.tokenTimeout
                    )

                    // Request for Auction-data at /auction
                    val auctionId = UUID.randomUUID().toString()
                    auctionStat.markAuctionStarted(auctionId, adTypeParam)
                    getAuctionRequest.request(
                        adTypeParam = adTypeParam,
                        auctionId = auctionId,
                        demandAd = demandAd,
                        adapters = adaptersSource.adapters.associate {
                            it.demandId.demandId to it.adapterInfo
                        },
                        tokens = tokens,
                    ).mapCatching { auctionData ->
                        if (auctionId != auctionData.auctionId) {
                            logError(TAG, "Auction ID has been changed", IllegalStateException())
                        }
                        resultsCollector.serverBiddingFinished(
                            auctionData.adUnits?.filter { it.bidType == BidType.RTB }
                        )
                        resultsCollector.setNoBidInfo(auctionData.noBids)
                        auctionData.printWaterfall(demandAd.adType)
                        val (results, auctionInfo) = conductAuction(
                            auctionData = auctionData,
                            demandAd = demandAd,
                            adTypeParamData = adTypeParam,
                            tokens = tokens,
                        )
                        if (results.isEmpty()) {
                            adTypeParam.activity.runOnUiThread {
                                onFailure(auctionInfo, BidonError.NoAuctionResults)
                            }
                        } else {
                            adTypeParam.activity.runOnUiThread {
                                onSuccess(results, auctionInfo)
                            }
                        }
                    }.onFailure { cause ->
                        logError(TAG, "Auction failed during execution", cause)
                        processAuctionFailed(adTypeParam, onFailure, cause)
                    }
                }.onFailure { cause ->
                    logError(TAG, "Auction failed", cause)
                    processAuctionFailed(adTypeParam, onFailure, cause)
                }
            }
        }
    }

    private suspend fun processAuctionFailed(
        adTypeParam: AdTypeParam,
        onFailure: (AuctionInfo?, Throwable) -> Unit,
        cause: Throwable
    ) {
        val statResult = proceedRoundResults()
        val auctionData = _auctionDataResponse
        if (auctionData == null) {
            logInfo(TAG, "No auction data response info.")
            adTypeParam.activity.runOnUiThread {
                onFailure(null, cause)
            }
        } else {
            val auctionInfo = getAuctionInfo(auctionData, statResult)
            printStatsData(auctionData, statResult, auctionInfo)
            adTypeParam.activity.runOnUiThread {
                onFailure(auctionInfo, cause)
            }
        }
        // Finish auction
        state.value = AuctionState.Finished
        clearData()
    }

    override fun cancel() {
        logInfo(TAG, "Trying to cancel auction. Is active: ${job?.isActive}")
        if (job?.isActive == true) {
            job?.cancel(AuctionCancellation())
            auctionStat.markAuctionCanceled()
            logInfo(TAG, "Auction canceled")
        }
        job = null
    }

    private fun getAuctionInfo(auctionData: AuctionResponse, statResult: RoundStat?): AuctionInfo {
        return AuctionInfo(
            auctionId = auctionData.auctionId,
            auctionConfigurationId = auctionData.auctionConfigurationId,
            auctionConfigurationUid = auctionData.auctionConfigurationUid,
            auctionPricefloor = auctionData.pricefloor,
            auctionTimeout = auctionData.auctionTimeout,
            noBids = statResult?.noBids?.map { it.toAuctionNoBidInfo() },
            adUnits = statResult?.demands?.map { it.toAuctionInfo() },
        )
    }

    private suspend fun conductAuction(
        auctionData: AuctionResponse,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
        tokens: Map<String, TokenInfo>,
    ): Pair<List<AuctionResult>, AuctionInfo> {
        _auctionDataResponse = auctionData
        _demandAd = demandAd
        val auctionPriceFloor = auctionData.pricefloor
        // Start auction
        executeAuction(
            auctionId = auctionData.auctionId,
            auctionConfigurationId = auctionData.auctionConfigurationId ?: 0L,
            auctionConfigurationUid = auctionData.auctionConfigurationUid ?: "",
            externalWinNotificationsEnabled = auctionData.externalWinNotificationsEnabled,
            auctionTimeout = auctionData.auctionTimeout,
            pricefloor = auctionPriceFloor,
            demandAd = demandAd,
            adTypeParam = adTypeParamData,
            adUnits = auctionData.adUnits ?: emptyList(),
            resultsCollector = resultsCollector,
            tokens = tokens,
        )

        resultsCollector.saveWinners(auctionPriceFloor)
        // Save round results
        val statResult = proceedRoundResults()

        val auctionInfo = getAuctionInfo(auctionData = auctionData, statResult = statResult)
        printStatsData(auctionData, statResult, auctionInfo)

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
            roundStat = statResult,
            demandAd = demandAd,
        )

        notifyWinLoss(finalResults)

        // Finish auction
        state.value = AuctionState.Finished
        // Wait for auction is completed
        state.first { it == AuctionState.Finished }
        val results = resultsCollector.getAll()
        clearData()
        return Pair(results, auctionInfo)
    }

    private fun printStatsData(
        auctionData: AuctionResponse,
        statResult: RoundStat?,
        auctionInfo: AuctionInfo
    ) {
        logInfo(
            TAG,
            "Was received: \nAdUnits: ${auctionData.adUnits?.size} \nNoBids: ${auctionData.noBids?.size}" +
                "\nWas sent:\nStats: ${statResult?.demands?.size} \nAuctionInfo AdUnits: ${auctionInfo.adUnits?.size} \n" +
                "AuctionInfo NoBids: ${auctionInfo.noBids?.size}"
        )
    }

    private suspend fun proceedRoundResults(): RoundStat? {
        (resultsCollector.getRoundResults() as? RoundResult.Results)?.let {
            return auctionStat.addRoundResults(it)
        }
        return null
    }

    private fun clearData() {
        logInfo(TAG, "Clearing data")
        resultsCollector.clear()
        _auctionDataResponse = null
    }

    private fun notifyWinLoss(finalResults: List<AuctionResult>) {
        val winner = finalResults.getOrNull(0) ?: return

        /**
         *  For internal statistics
         */
        winner.adSource.markWin()

        /**
         * For AdNetworks
         */
        (winner.adSource as? WinLossNotifiable)?.notifyWin()

        finalResults.drop(1)
            .forEach { auctionResult ->
                val adSource = auctionResult.adSource
                /**
                 *  Bidding demands should not be notified.
                 */
                if (auctionResult !is AuctionResult.Bidding && adSource is WinLossNotifiable) {
                    logInfo(TAG, "Notified loss: ${adSource.demandId}")
                    adSource.notifyLoss(
                        winner.adSource.demandId.demandId,
                        winner.adSource.getStats().ecpm
                    )
                }
                if (auctionResult.roundStatus == RoundStatus.Successful) {
                    adSource.markLoss()
                }
                logInfo(TAG, "Destroying loser: ${adSource.demandId}")
                adSource.destroy()
            }
    }
}

private const val TAG = "Auction"
