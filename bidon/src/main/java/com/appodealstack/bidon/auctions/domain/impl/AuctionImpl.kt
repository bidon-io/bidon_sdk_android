package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.*
import com.appodealstack.bidon.auctions.domain.*
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.withTimeoutOrNull

internal class AuctionImpl(
    private val adaptersSource: AdaptersSource,
    private val getAuctionRequest: GetAuctionRequestUseCase
) : Auction {
    private val state = MutableStateFlow(AuctionState.Initialized)
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())
    private val mutableLineItems = mutableListOf<LineItem>()

    override suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeAdditionalData: AdTypeAdditional
    ): Result<List<AuctionResult>> = runCatching {
        if (state.getAndUpdate { AuctionState.InProgress } == AuctionState.Initialized) {
            println("++++ 1")
            logInfo(Tag, "Action started $this")
            println("++++ 2")
            // Request for Auction-data at /auction
            val auctionData = requestActionData()
            mutableLineItems.addAll(auctionData.lineItems ?: emptyList())

            // Start auction
            conductRounds(
                rounds = auctionData.rounds ?: listOf(),
                minPriceFloor = auctionData.minPrice ?: 0.0,
                priceFloor = auctionData.minPrice ?: 0.0,
                auctionData = auctionData,
                roundsListener = roundsListener,
                resolver = resolver,
                demandAd = demandAd,
                adTypeAdditionalData = adTypeAdditionalData
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
                }
                .onSuccess {
                    logInfo(Tag, "Winner filled: ${auctionResult.adSource.demandId}")
                    (auctionResult.adSource as? WinLossNotifiable)?.let {
                        logInfo(Tag, "Notified win: ${auctionResult.adSource.demandId}")
                        it.notifyWin()
                    }
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
        adTypeAdditionalData: AdTypeAdditional,
        auctionData: AuctionResponse
    ) {
        val round = rounds.firstOrNull() ?: return
        roundsListener.roundStarted(round.id)

        executeRound(
            round = round,
            priceFloor = priceFloor,
            demandAd = demandAd,
            adTypeAdditionalData = adTypeAdditionalData,
            timeout = round.timeoutMs
        ).onSuccess { roundResults ->
            saveAuctionResults(
                resolver = resolver,
                roundResults = roundResults.filter {
                    /**
                     * Received price should not be less then initial one [minPriceFloor].
                     */
                    it.priceFloor >= minPriceFloor
                }
            )
            roundsListener.roundSucceed(
                roundId = round.id,
                roundResults = roundResults
            )
        }.onFailure {
            logError(Tag, "Round '${round.id}' failed", it)
            roundsListener.roundFailed(
                roundId = round.id,
                error = it
            )
        }

        val nextPriceFloor = auctionResults.value.firstOrNull()?.priceFloor ?: priceFloor
        conductRounds(
            rounds = rounds.drop(1),
            minPriceFloor = minPriceFloor,
            priceFloor = nextPriceFloor,
            roundsListener = roundsListener,
            resolver = resolver,
            demandAd = demandAd,
            adTypeAdditionalData = adTypeAdditionalData,
            auctionData = auctionData
        )
    }

    private suspend fun executeRound(
        round: Round,
        priceFloor: Double,
        demandAd: DemandAd,
        adTypeAdditionalData: AdTypeAdditional,
        timeout: Long
    ): Result<List<AuctionResult>> = coroutineScope {
        runCatching {
            val filteredAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in round.demandIds
            }
            (round.demandIds - filteredAdapters.map { it.demandId.demandId }.toSet())
                .takeIf { it.isNotEmpty() }
                ?.let { logError(Tag, "Adapters not found: $it", NoSuchElementException(it.joinToString())) }
            logInfo(Tag, "Round '${round.id}' started with adapters [${filteredAdapters.joinToString { it.demandId.demandId }}]")
            logInfo(Tag, "Round '${round.id}' started with line items: $mutableLineItems")
            val adSources = when (demandAd.adType) {
                AdType.Interstitial -> {
                    filteredAdapters.filterIsInstance<AdProvider.Interstitial<AdAuctionParams>>().map {
                        it.interstitial(demandAd, round.id)
                    }
                }
                AdType.Rewarded -> {
                    filteredAdapters.filterIsInstance<AdProvider.Rewarded<AdAuctionParams>>().map {
                        it.rewarded(demandAd, round.id)
                    }
                }
                AdType.Banner -> {
                    filteredAdapters.filterIsInstance<AdProvider.Banner<AdAuctionParams>>().map {
                        it.banner(demandAd, round.id)
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
                                adSource,
                                adTypeAdditionalData,
                                priceFloor,
                                timeout,
                                availableLineItemsForDemand
                            )
                            adParam.getOrNull()?.let {
                                adSource.bid(adParams = it)
                            } ?: BidonError.NoAppropriateAdUnitId.asFailure()
                        } ?: BidonError.BidTimedOut(adSource.demandId).asFailure()
                    } to adSource
                }.mapIndexedNotNull { index, (deferred, adSource) ->
                    val logRoundTitle = "Round '${round.id}' result #$index(${adSource.demandId.demandId})"
                    deferred.await()
                        .onSuccess { auctionResult ->
                            logInfo(Tag, "$logRoundTitle: $auctionResult")
                        }
                        .onFailure { cause ->
                            logError(Tag, "$logRoundTitle: error while receiving bid.", cause)
                        }.getOrNull()
                }.also {
                    logInfo(Tag, "Round '${round.id}' finished with ${it.size} results: $it")
                }
        }
    }

    private fun obtainAdParamByType(
        adSource: AdSource<AdAuctionParams>,
        adTypeAdditionalData: AdTypeAdditional,
        priceFloor: Double,
        timeout: Long,
        availableLineItemsForDemand: List<LineItem>
    ) = when (adSource) {
        is AdSource.Banner -> {
            check(adTypeAdditionalData is AdTypeAdditional.Banner)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                adContainer = adTypeAdditionalData.adContainer,
                bannerSize = adTypeAdditionalData.bannerSize,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                }
            )
        }
        is AdSource.Interstitial -> {
            check(adTypeAdditionalData is AdTypeAdditional.Interstitial)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                activity = adTypeAdditionalData.activity,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                }
            )
        }
        is AdSource.Rewarded -> {
            check(adTypeAdditionalData is AdTypeAdditional.Rewarded)
            adSource.getAuctionParams(
                priceFloor = priceFloor,
                timeout = timeout,
                lineItems = availableLineItemsForDemand,
                activity = adTypeAdditionalData.activity,
                onLineItemConsumed = { lineItem ->
                    mutableLineItems.remove(lineItem)
                }
            )
        }
    }

    private suspend fun saveAuctionResults(resolver: AuctionResolver, roundResults: List<AuctionResult>) {
        auctionResults.value = resolver.sortWinners(auctionResults.value + roundResults)
    }

    private suspend fun requestActionData(): AuctionResponse {
        val auctionResponse = getAuctionRequest.request().getOrNull()
        return requireNotNull(auctionResponse) {
            "No auction data response"
        }
    }

    private fun List<LineItem>.filterBy(demandId: DemandId) =
        this.filter { it.demandId == demandId.demandId }
}

private const val Tag = "Auction"
private const val DefaultFillTimeoutMs = 10_000L
private const val NoWinnerFilled = -1
