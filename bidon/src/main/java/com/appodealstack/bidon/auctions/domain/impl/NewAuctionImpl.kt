package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.*
import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.auctions.domain.GetAuctionRequestUseCase
import com.appodealstack.bidon.auctions.domain.NewAuction
import com.appodealstack.bidon.auctions.domain.RoundsListener
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.withTimeoutOrNull

internal class NewAuctionImpl(
    private val adaptersSource: AdaptersSource,
    private val contextProvider: ContextProvider,
    private val getAuctionRequest: GetAuctionRequestUseCase
) : NewAuction {
    private val isAuctionActive = MutableStateFlow(true)
    private val isAuctionStarted = MutableStateFlow(false)
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())

    override val isActive: Boolean
        get() = isAuctionActive.value

    override suspend fun start(
        demandAd: DemandAd,
        resolver: AuctionResolver,
        roundsListener: RoundsListener,
        adTypeAdditionalData: AdTypeAdditional
    ): Result<List<AuctionResult>> = runCatching {
        if (!isAuctionStarted.getAndUpdate { true }) {
            // Request for Auction-data /auction
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

            // Finish auction
            isAuctionActive.value = false
        }
        isAuctionActive.first { !it }

        val finalResults = fillWinner(auctionResults.value).also {
            auctionResults.value = it
        }
        notifyLosers(finalResults)
        finalResults.ifEmpty {
            throw BidonError.AuctionFailed
        }
    }

    private fun notifyLosers(finalResults: List<AuctionResult>) {
        finalResults.drop(1).forEach {
            when (val sourceAd = it.adSource) {
                is AdSource.Interstitial<*> -> {
                    logInfo(Tag, "Notified loss: ${it.adSource.demandId}")
                    sourceAd.notifyLoss()
                }
            }
        }
    }

    private suspend fun fillWinner(auctionResults: List<AuctionResult>): List<AuctionResult> {
        val index = auctionResults.indexOfFirst {
            when (val adSource = it.adSource) {
                is AdSource.Interstitial<*> -> {
                    adSource.fill()
                        .onFailure {
                            logInfo(Tag, "Notified loss: ${adSource.demandId}")
                            adSource.notifyLoss()
                        }.onSuccess {
                            logInfo(Tag, "Notified loss: ${adSource.demandId}")
                            adSource.notifyWin()
                        }.isSuccess
                }
            }
        }
        return auctionResults.drop(index)
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
        val auctionRequests = when (demandAd.adType) {
            AdType.Interstitial -> {
                check(adTypeAdditionalData is AdTypeAdditional.Interstitial)
                filteredAdapters.filterIsInstance<AdProvider.Interstitial<AdSource.AdParams>>().map {
                    it.interstitial(demandAd, round.id)
                }
            }
            AdType.Banner -> TODO()
            AdType.Rewarded -> TODO()
        }
        auctionRequests
            .map { auctionRequest ->
                coroutineScope {
                    async {
                        withTimeoutOrNull(round.timeoutMs) {
                            auctionRequest.bid(
                                activity = contextProvider.activity,
                                adParams = auctionRequest.getParams(
                                    priceFloor = priceFloor,
                                    timeout = timeout,
                                    lineItems = lineItems
                                )
                            )
                        }
                    }
                }
            }.mapNotNull { deferred ->
                deferred.await()?.getOrNull()?.result
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