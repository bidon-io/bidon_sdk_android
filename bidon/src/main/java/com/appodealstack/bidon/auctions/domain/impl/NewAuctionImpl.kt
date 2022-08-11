package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.NewAuction
import com.appodealstack.bidon.auctions.domain.RoundsListener
import com.appodealstack.bidon.auctions.data.models.AuctionResponse
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.data.models.Round
import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ContextProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.withTimeoutOrNull

internal class NewAuctionImpl(
    private val adaptersSource: AdaptersSource,
    private val contextProvider: ContextProvider,
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
                priceFloor = auctionData.minPrice ?: 0.0,
                roundsListener = roundsListener,
                resolver = resolver,
                demandAd = demandAd,
                adTypeAdditionalData = adTypeAdditionalData
            )

            // Finish auction
            isAuctionActive.value = false
            auctionResults.value
        } else {
            waitForResult()
        }
    }

    private suspend fun conductRounds(
        rounds: List<Round>,
        lineItems: List<LineItem>,
        priceFloor: Double,
        roundsListener: RoundsListener,
        resolver: AuctionResolver,
        demandAd: DemandAd,
        adTypeAdditionalData: AdTypeAdditional
    ) {
        val round = rounds.firstOrNull() ?: return
        roundsListener.roundStarted(round.id)

        executeRound(
            round = round,
            lineItems = lineItems,
            priceFloor = priceFloor,
            demandAd = demandAd,
            adTypeAdditionalData = adTypeAdditionalData
        ).onSuccess { roundResults ->
            saveAuctionResults(resolver, roundResults)
            roundsListener.roundSucceed(round.id, roundResults)
        }.onFailure {
            roundsListener.roundFailed(round.id, it)
        }

        val nextPriceFloor = auctionResults.value.firstOrNull()?.ad?.price ?: priceFloor
        conductRounds(
            rounds = rounds.drop(1),
            lineItems = lineItems,
            priceFloor = nextPriceFloor,
            roundsListener = roundsListener,
            resolver = resolver,
            demandAd = demandAd,
            adTypeAdditionalData = adTypeAdditionalData
        )
    }

    private suspend fun executeRound(
        round: Round,
        lineItems: List<LineItem>,
        priceFloor: Double,
        demandAd: DemandAd,
        adTypeAdditionalData: AdTypeAdditional
    ): Result<List<AuctionResult>> = runCatching {
        val filteredAdapters = adaptersSource.adapters.filter {
            it.demandId.demandId in round.demandIds
        }
        val auctionRequests = when (demandAd.adType) {
            AdType.Banner -> {
                check(adTypeAdditionalData is AdTypeAdditional.Banner)
                filteredAdapters.filterIsInstance<AdSource.Banner<AdSource.AdParams>>().map {
                    it.banner(
                        demandAd = demandAd,
                        context = contextProvider.requiredContext,
                        adParams = it.bannerParams(
                            lineItems = lineItems,
                            priceFloor = priceFloor,
                            adContainer = adTypeAdditionalData.adContainer,
                            bannerSize = adTypeAdditionalData.bannerSize
                        )
                    )
                }
            }
            AdType.Interstitial -> {
                check(adTypeAdditionalData is AdTypeAdditional.Interstitial)
                filteredAdapters.filterIsInstance<AdSource.Interstitial<AdSource.AdParams>>().map {
                    it.interstitial(
                        demandAd = demandAd,
                        adParams = it.interstitialParams(
                            priceFloor = priceFloor,
                            lineItems = lineItems,
                        ),
                        activity = adTypeAdditionalData.activity,
                    )
                }
            }
            AdType.Rewarded -> {
                check(adTypeAdditionalData is AdTypeAdditional.Rewarded)
                filteredAdapters.filterIsInstance<AdSource.Rewarded<AdSource.AdParams>>().map {
                    it.rewarded(
                        demandAd = demandAd,
                        adParams = it.rewardedParams(
                            priceFloor = priceFloor,
                            lineItems = lineItems,
                        ),
                        activity = adTypeAdditionalData.activity,
                    )
                }
            }
        }
        auctionRequests
            .map { auctionRequest ->
                coroutineScope {
                    async {
                        withTimeoutOrNull(round.timeoutMs) {
                            auctionRequest.execute()
                        }
                    }
                }
            }.mapNotNull { deferred ->
                deferred.await()?.getOrNull()
            }
    }

    private suspend fun saveAuctionResults(resolver: AuctionResolver, roundResults: List<AuctionResult>) {
        auctionResults.value = resolver.sortWinners(auctionResults.value + roundResults)
    }

    private suspend fun requestActionData(): AuctionResponse {
        TODO("Not yet implemented")
    }

    private suspend fun waitForResult(): List<AuctionResult> {
        isAuctionActive.first { !it }
        return auctionResults.value
    }

}