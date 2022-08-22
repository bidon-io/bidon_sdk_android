package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.*
import com.appodealstack.bidon.auctions.domain.*
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ContextProvider
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
    private val contextProvider: ContextProvider,
    private val getAuctionRequest: GetAuctionRequestUseCase
) : Auction {
    private val state = MutableStateFlow(AuctionState.Initialized)
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())

    override val results: List<AuctionResult>
        get() = auctionResults.value.takeIf {
            state.value == AuctionState.Finished
        }.orEmpty()

    override val isActive: Boolean get() = state.value == AuctionState.InProgress

    override suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeAdditionalData: AdTypeAdditional
    ): Result<List<AuctionResult>> = runCatching {
        check(state.value != AuctionState.Destroyed) {
            "Auction is already destroyed."
        }
        if (state.getAndUpdate { AuctionState.InProgress } == AuctionState.Initialized) {
            logInfo(Tag, "Action started $this")
            // Request for Auction-data at /auction
            val auctionData = requestActionData()

            // Start auction
            conductRounds(
                rounds = auctionData.rounds ?: listOf(),
                lineItems = auctionData.lineItems ?: listOf(),
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
            throw BidonError.AuctionFailed
        }
    }

    override fun destroy() {
        logInfo(Tag, "Destroyed")
        state.value = AuctionState.Destroyed
        auctionResults.value.forEach {
            it.adSource.destroy()
        }
        auctionResults.value = emptyList()
    }

    private fun notifyLosers(finalResults: List<AuctionResult>) {
        finalResults.drop(1)
            .map { it.adSource }
            .filterIsInstance<WinLossNotifiable>()
            .forEach {
                logInfo(Tag, "Notified loss: ${(it as? AdSource<*>)?.demandId}")
                it.notifyLoss()
            }
    }

    private suspend fun fillWinner(auctionResults: List<AuctionResult>, timeout: Long): List<AuctionResult> {
        val index = auctionResults.indexOfFirst { auctionResult ->
            when (val adSource = auctionResult.adSource) {
                is AdSource.Interstitial<*> -> {
                    logInfo(Tag, "Filling winner started for auction result: $auctionResult")
                    val fillResult = withTimeoutOrNull(timeout) {
                        adSource.fill()
                    } ?: BidonError.FillTimedOut(auctionResult.adSource.demandId).asFailure()

                    fillResult
                        .onFailure { cause ->
                            logError(Tag, "Failed to fill: ${adSource.demandId}", cause)
                            (adSource as? WinLossNotifiable)?.let {
                                logInfo(Tag, "Notified loss: ${adSource.demandId}")
                                it.notifyLoss()
                            }
                        }
                        .onSuccess {
                            logInfo(Tag, "Winner filled: ${adSource.demandId}")
                            (adSource as? WinLossNotifiable)?.let {
                                logInfo(Tag, "Notified win: ${adSource.demandId}")
                                it.notifyWin()
                            }
                        }
                        .isSuccess
                }
                is AdSource.Rewarded<*> -> {
                    logInfo(Tag, "Filling winner started for auction result: $auctionResult")
                    val fillResult = withTimeoutOrNull(timeout) {
                        adSource.fill()
                    } ?: BidonError.FillTimedOut(auctionResult.adSource.demandId).asFailure()

                    fillResult
                        .onFailure { cause ->
                            logError(Tag, "Failed to fill: ${adSource.demandId}", cause)
                            (adSource as? WinLossNotifiable)?.let {
                                logInfo(Tag, "Notified loss: ${adSource.demandId}")
                                it.notifyLoss()
                            }
                        }
                        .onSuccess {
                            logInfo(Tag, "Winner filled: ${adSource.demandId}")
                            (adSource as? WinLossNotifiable)?.let {
                                logInfo(Tag, "Notified win: ${adSource.demandId}")
                                it.notifyWin()
                            }
                        }
                        .isSuccess
                }
                is AdSource.Banner -> TODO()
            }
        }
        return if (index == -1) auctionResults
        else auctionResults.drop(index)
    }

    private suspend fun conductRounds(
        rounds: List<Round>,
        lineItems: List<LineItem>,
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
            lineItems = lineItems,
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
                })
            roundsListener.roundSucceed(
                roundId = round.id,
                roundResults = roundResults
            )
        }.onFailure {
            roundsListener.roundFailed(
                roundId = round.id,
                error = it
            )
        }

        val nextPriceFloor = auctionResults.value.firstOrNull()?.priceFloor ?: priceFloor
        conductRounds(
            rounds = rounds.drop(1),
            lineItems = lineItems,
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
        lineItems: List<LineItem>,
        priceFloor: Double,
        demandAd: DemandAd,
        adTypeAdditionalData: AdTypeAdditional,
        timeout: Long
    ): Result<List<AuctionResult>> = runCatching {
        val filteredAdapters = adaptersSource.adapters.filter {
            it.demandId.demandId in round.demandIds
        }
        logInfo(Tag, "Round '${round.id}' started with adapters [${filteredAdapters.joinToString { it.demandId.demandId }}]")
        val auctionRequests = when (demandAd.adType) {
            AdType.Interstitial -> {
                check(adTypeAdditionalData is AdTypeAdditional.Interstitial)
                filteredAdapters.filterIsInstance<AdProvider.Interstitial<AdAuctionParams>>().map {
                    it.interstitial(demandAd, round.id)
                }
            }
            AdType.Rewarded -> {
                check(adTypeAdditionalData is AdTypeAdditional.Rewarded)
                filteredAdapters.filterIsInstance<AdProvider.Rewarded<AdAuctionParams>>().map {
                    it.rewarded(demandAd, round.id)
                }
            }
            AdType.Banner -> TODO()
        }
        auctionRequests
            .map { auctionRequest ->
                logInfo(Tag, "Round '${round.id}'. Adapter ${auctionRequest.demandId.demandId} starts bidding")
                coroutineScope {
                    async {
                        withTimeoutOrNull(round.timeoutMs) {
                            auctionRequest.bid(
                                activity = contextProvider.activity,
                                adParams = auctionRequest.getAuctionParams(
                                    priceFloor = priceFloor,
                                    timeout = timeout,
                                    lineItems = lineItems
                                )
                            )
                        } ?: BidonError.BidTimedOut(auctionRequest.demandId).asFailure()
                    }
                }
            }.mapIndexedNotNull { index, deferred ->
                deferred.await()
                    .onSuccess { auctionResult ->
                        logInfo(Tag, "Round '${round.id}' result #$index: $auctionResult")
                    }
                    .onFailure {
                        logError(Tag, "Round '${round.id}'. Error while receiving bid.", it)
                    }.getOrNull()
            }.also {
                logInfo(Tag, "Round '${round.id}' finished with ${it.size} results: $it")
            }
    }

    private suspend fun saveAuctionResults(resolver: AuctionResolver, roundResults: List<AuctionResult>) {
        auctionResults.value = resolver.sortWinners(auctionResults.value + roundResults)
    }

    private suspend fun requestActionData(): AuctionResponse {
        val auctionResponse = getAuctionRequest.request()
            .onFailure {
                logError(Tag, "Error while loading auction data", it)
            }.onSuccess {
                logInfo(Tag, "Loaded auction data: $it")
            }
            .getOrNull()
        return requireNotNull(auctionResponse) {
            "Something bad happened"
        }
    }
}

private const val Tag = "Auction"
private const val DefaultFillTimeoutMs = 10000L