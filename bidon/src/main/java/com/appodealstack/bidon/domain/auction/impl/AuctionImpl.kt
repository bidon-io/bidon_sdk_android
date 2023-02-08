package com.appodealstack.bidon.domain.auction.impl

import com.appodealstack.bidon.data.models.auction.AuctionResponse
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.auction.Round
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.domain.adapter.*
import com.appodealstack.bidon.domain.auction.*
import com.appodealstack.bidon.domain.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.DemandId
import com.appodealstack.bidon.domain.common.ext.asFailure
import com.appodealstack.bidon.domain.stats.DemandStat
import com.appodealstack.bidon.domain.stats.RoundStat
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.domain.stats.usecases.StatsRequestUseCase
import com.appodealstack.bidon.view.helper.SdkDispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AuctionImpl(
    private val adaptersSource: AdaptersSource,
    private val getAuctionRequest: GetAuctionRequestUseCase,
    private val statsRequest: StatsRequestUseCase,
) : Auction {
    private val state = MutableStateFlow(AuctionState.Initialized)
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())
    private val statsRound = mutableListOf<RoundStat>()
    private val statsAuctionResults = mutableListOf<AuctionResult>()
    private val mutableLineItems = mutableListOf<LineItem>()
    private var _auctionDataResponse: AuctionResponse? = null
    private val auctionDataResponse: AuctionResponse
        get() = requireNotNull(_auctionDataResponse)

    override suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeParamData: AdTypeParam
    ): Result<List<AuctionResult>> = runCatching {
        if (state.compareAndSet(expect = AuctionState.Initialized, update = AuctionState.InProgress)) {
            logInfo(Tag, "Action started $this")
            // Request for Auction-data at /auction
            getAuctionRequest.request(
                placement = demandAd.placement,
                additionalData = adTypeParamData,
                auctionId = UUID.randomUUID().toString(),
                adapters = adaptersSource.adapters.associate {
                    it.demandId.demandId to it.adapterInfo
                }
            ).onSuccess { auctionData ->
                _auctionDataResponse = auctionData
                mutableLineItems.addAll(auctionData.lineItems ?: emptyList())
                // Start auction
                conductRounds(
                    rounds = auctionData.rounds ?: listOf(),
                    minPriceFloor = auctionData.minPrice ?: 0.0,
                    priceFloor = auctionData.minPrice ?: 0.0,
                    roundsListener = roundsListener,
                    resolver = resolver,
                    demandAd = demandAd,
                    adTypeParamData = adTypeParamData
                )
                logInfo(Tag, "Rounds completed")

                // Finding winner
                val finalResults = fillWinner(
                    auctionResults = auctionResults.value,
                    timeout = auctionData.fillTimeout ?: DefaultFillTimeoutMs
                ).also {
                    auctionResults.value = it
                }
                logInfo(Tag, "Action finished with ${finalResults.size} results")
                finalResults.forEachIndexed { index, auctionResult ->
                    logInfo(Tag, "Action result #$index: $auctionResult")
                }
                notifyLosers(finalResults)

                // Finish auction
                state.value = AuctionState.Finished
                sendStatsAsync(demandAd.adType)
            }.getOrThrow()
        }
        state.first { it == AuctionState.Finished }

        auctionResults.value.ifEmpty {
            throw BidonError.NoAuctionResults
        }
    }

    private fun notifyLosers(finalResults: List<AuctionResult>) {
        finalResults.drop(1)
            .map { it.adSource }
            .forEach { adSource ->
                if (adSource is WinLossNotifiable) {
                    logInfo(Tag, "Notified loss: ${adSource.demandId}")
                    adSource.notifyLoss()
                }
                (adSource as StatisticsCollector).markLoss()
                logInfo(Tag, "Destroying loser: ${adSource.demandId}")
                adSource.destroy()
            }
    }

    private suspend fun fillWinner(auctionResults: List<AuctionResult>, timeout: Long): List<AuctionResult> {
        val index = auctionResults.indexOfFirst { auctionResult ->
            val fillResult = withTimeoutOrNull(timeout) {
                logInfo(Tag, "Filling winner started for auction result: $auctionResult")
                auctionResult.adSource.fill()
            } ?: BidonError.FillTimedOut(auctionResult.adSource.demandId).asFailure()

            fillResult
                .onFailure { cause ->
                    logError(Tag, "Failed to fill: ${auctionResult.adSource.demandId}", cause)
                    (auctionResult.adSource as? WinLossNotifiable)?.let {
                        logInfo(Tag, "Notified loss: ${auctionResult.adSource.demandId}")
                        it.notifyLoss()
                    }
                    (auctionResult.adSource as StatisticsCollector).markLoss()
                }
                .onSuccess {
                    logInfo(Tag, "Winner filled: ${auctionResult.adSource.demandId}")
                    (auctionResult.adSource as? WinLossNotifiable)?.let {
                        logInfo(Tag, "Notified win: ${auctionResult.adSource.demandId}")
                        it.notifyWin()
                    }
                    (auctionResult.adSource as StatisticsCollector).markWin()
                }
                .isSuccess
        }
        return if (index == NoWinnerFilled) emptyList()
        else auctionResults.drop(index)
    }

    private suspend fun conductRounds(
        rounds: List<Round>,
        minPriceFloor: Double,
        priceFloor: Double,
        roundsListener: RoundsListener,
        resolver: AuctionResolver,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
    ) {
        val round = rounds.firstOrNull() ?: return
        roundsListener.roundStarted(round.id)

        val allRoundResults = executeRound(
            round = round,
            priceFloor = priceFloor,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
            timeout = round.timeoutMs
        ).getOrNull() ?: emptyList()

        proceedRoundResults(
            resolver = resolver,
            allResults = allRoundResults,
            minPriceFloor = minPriceFloor,
            round = round,
            priceFloor = priceFloor,
            roundsListener = roundsListener
        )

        val nextPriceFloor = auctionResults.value.firstOrNull()?.ecpm ?: priceFloor
        conductRounds(
            rounds = rounds.drop(1),
            minPriceFloor = minPriceFloor,
            priceFloor = nextPriceFloor,
            roundsListener = roundsListener,
            resolver = resolver,
            demandAd = demandAd,
            adTypeParamData = adTypeParamData,
        )
    }

    private suspend fun proceedRoundResults(
        resolver: AuctionResolver,
        allResults: List<AuctionResult>,
        minPriceFloor: Double,
        round: Round,
        priceFloor: Double,
        roundsListener: RoundsListener
    ) {
        val sortedResult = resolver.sortWinners(allResults)
        val successfulResults = sortedResult
            .filter { (it.adSource as StatisticsCollector).buildBidStatistic().roundStatus == RoundStatus.Successful }
            .filter {
                /**
                 * Received ecpm should not be less then initial one [minPriceFloor].
                 */
                val isAbovePricefloor = it.ecpm >= minPriceFloor
                if (!isAbovePricefloor) {
                    (it.adSource as StatisticsCollector).markBelowPricefloor()
                }
                isAbovePricefloor
            }

        /**
         * Save statistic data for /stats
         */
        saveStatistics(
            round = round,
            priceFloor = priceFloor,
            allRoundResults = allResults,
            sortedRoundResult = sortedResult,
            successfulRoundResults = successfulResults,
        )

        /**
         * Save auction results data
         */
        if (successfulResults.isNotEmpty()) {
            saveAuctionResults(
                resolver = resolver,
                roundResults = successfulResults
            )
            roundsListener.roundSucceed(
                roundId = round.id,
                roundResults = successfulResults
            )
        } else {
            logError(Tag, "Round '${round.id}' failed", BidonError.NoRoundResults)
            roundsListener.roundFailed(
                roundId = round.id,
                error = BidonError.NoRoundResults
            )
        }
    }

    private fun saveStatistics(
        round: Round,
        priceFloor: Double,
        allRoundResults: List<AuctionResult>,
        sortedRoundResult: List<AuctionResult>,
        successfulRoundResults: List<AuctionResult>,
    ) {
        val winner = successfulRoundResults.firstOrNull()
        val unknownDemandId = (round.demandIds - allRoundResults.map { it.adSource.demandId.demandId }.toSet()).takeIf {
            it.isNotEmpty()
        }?.map { demandId ->
            DemandStat(
                roundStatus = RoundStatus.UnknownAdapter,
                demandId = DemandId(demandId),
                bidStartTs = null,
                bidFinishTs = null,
                fillStartTs = null,
                fillFinishTs = null,
                ecpm = null,
                adUnitId = null
            )
        } ?: emptyList()

        allRoundResults.forEach {
            (it.adSource as StatisticsCollector).addAuctionConfigurationId(
                auctionConfigurationId = auctionDataResponse.auctionConfigurationId ?: 0
            )
        }

        val roundStat = RoundStat(
            auctionId = auctionDataResponse.auctionId ?: "",
            roundId = round.id,
            priceFloor = priceFloor,
            winnerDemandId = winner?.adSource?.demandId,
            winnerEcpm = winner?.ecpm,
            demands = unknownDemandId
        )
        statsAuctionResults.addAll(sortedRoundResult)
        statsRound.add(roundStat)
    }

    private suspend fun sendStatsAsync(adType: AdType) {
        coroutineScope {
            launch(SdkDispatchers.Default) {
                val bidStats = statsAuctionResults.map {
                    (it.adSource as StatisticsCollector).buildBidStatistic()
                }
                statsRequest.invoke(
                    auctionId = auctionDataResponse.auctionId ?: "",
                    auctionConfigurationId = auctionDataResponse.auctionConfigurationId ?: -1,
                    results = statsRound.map { roundStat ->
                        val errorDemandStat = roundStat.demands
                        val succeedDemandStat = bidStats.filter { it.roundId == roundStat.roundId }
                            .map {
                                DemandStat(
                                    roundStatus = requireNotNull(it.roundStatus),
                                    demandId = it.demandId,
                                    bidStartTs = it.bidStartTs,
                                    bidFinishTs = it.bidFinishTs,
                                    fillStartTs = it.fillStartTs,
                                    fillFinishTs = it.fillFinishTs,
                                    ecpm = it.ecpm,
                                    adUnitId = it.adUnitId
                                )
                            }
                        roundStat.copy(
                            demands = (succeedDemandStat + errorDemandStat).map { demandStat ->
                                if (demandStat.roundStatus == RoundStatus.Successful) {
                                    demandStat.copy(
                                        roundStatus = RoundStatus.Loss
                                    )
                                } else {
                                    demandStat
                                }
                            }
                        )
                    },
                    adType = adType
                )
                statsRound.clear()
            }
        }
    }

    private suspend fun executeRound(
        round: Round,
        priceFloor: Double,
        demandAd: DemandAd,
        adTypeParamData: AdTypeParam,
        timeout: Long
    ): Result<List<AuctionResult>> = coroutineScope {
        runCatching {
            val filteredAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in round.demandIds
            }
            (round.demandIds - filteredAdapters.map { it.demandId.demandId }.toSet())
                .takeIf { it.isNotEmpty() }
                ?.let { unknownDemandIds ->
                    logError(
                        tag = Tag,
                        message = "Adapters not found: $unknownDemandIds",
                        error = NoSuchElementException(unknownDemandIds.joinToString())
                    )
                }
            logInfo(Tag, "Round '${round.id}' started with adapters [${filteredAdapters.joinToString { it.demandId.demandId }}]")
            logInfo(Tag, "Round '${round.id}' started with line items: $mutableLineItems")
            val adSources = when (demandAd.adType) {
                AdType.Interstitial -> {
                    filteredAdapters.filterIsInstance<AdProvider.Interstitial<AdAuctionParams>>().map {
                        it.interstitial(demandAd = demandAd, roundId = round.id, auctionId = auctionDataResponse.auctionId ?: "")
                    }
                }
                AdType.Rewarded -> {
                    filteredAdapters.filterIsInstance<AdProvider.Rewarded<AdAuctionParams>>().map {
                        it.rewarded(demandAd = demandAd, roundId = round.id, auctionId = auctionDataResponse.auctionId ?: "")
                    }
                }
                AdType.Banner -> {
                    filteredAdapters.filterIsInstance<AdProvider.Banner<AdAuctionParams>>().map {
                        it.banner(demandAd = demandAd, roundId = round.id, auctionId = auctionDataResponse.auctionId ?: "")
                    }
                }
            }
            adSources
                .map { adSource ->
                    val availableLineItemsForDemand = mutableLineItems.filterBy(adSource.demandId)
                    logInfo(
                        tag = Tag,
                        message = "Round '${round.id}'. Adapter ${adSource.demandId.demandId} starts bidding. " +
                            "Min PriceFloor=$priceFloor. LineItems: $availableLineItemsForDemand."
                    )
                    async {
                        withTimeoutOrNull(round.timeoutMs) {
                            val adParam = obtainAdParamByType(
                                adSource = adSource,
                                adTypeParamData = adTypeParamData,
                                priceFloor = priceFloor,
                                timeout = timeout,
                                availableLineItemsForDemand = availableLineItemsForDemand,
                            )
                            adParam.getOrNull()?.let {
                                adSource.bid(adParams = it)
                            } ?: RoundStatus.NoAppropriateAdUnitId.asAuctionResult(adSource)
                        } ?: RoundStatus.BidTimeoutReached.asAuctionResult(adSource)
                    } to adSource
                }.mapIndexed { index, (deferred, adSource) ->
                    val logRoundTitle = "Round '${round.id}' result #$index(${adSource.demandId.demandId})"
                    deferred.await().also {
                        logInfo(Tag, "$logRoundTitle: $it. Statistics: ${(adSource as StatisticsCollector).buildBidStatistic()}")
                    }
                }.also {
                    logInfo(Tag, "Round '${round.id}' finished with ${it.size} results: $it")
                }
        }
    }

    private fun RoundStatus.asAuctionResult(adSource: AdSource<AdAuctionParams>): AuctionResult {
        (adSource as StatisticsCollector).markBidFinished(
            roundStatus = this,
            ecpm = null
        )
        return AuctionResult(
            ecpm = 0.0,
            adSource = adSource
        )
    }

    private fun obtainAdParamByType(
        adSource: AdSource<AdAuctionParams>,
        adTypeParamData: AdTypeParam,
        priceFloor: Double,
        timeout: Long,
        availableLineItemsForDemand: List<LineItem>,
    ) = when (adSource) {
        is AdSource.Banner -> {
            check(adTypeParamData is AdTypeParam.Banner)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                adContainer = adTypeParamData.adContainer,
                bannerSize = adTypeParamData.bannerSize,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                },
            )
        }
        is AdSource.Interstitial -> {
            check(adTypeParamData is AdTypeParam.Interstitial)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                activity = adTypeParamData.activity,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                },
            )
        }
        is AdSource.Rewarded -> {
            check(adTypeParamData is AdTypeParam.Rewarded)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                activity = adTypeParamData.activity,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                },
            )
        }
    }

    private suspend fun saveAuctionResults(resolver: AuctionResolver, roundResults: List<AuctionResult>) {
        auctionResults.value = resolver.sortWinners(auctionResults.value + roundResults)
    }

    private fun List<LineItem>.filterBy(demandId: DemandId) =
        this.filter { it.demandId == demandId.demandId }
}

private const val Tag = "Auction"
private const val DefaultFillTimeoutMs = 10_000L
private const val NoWinnerFilled = -1
