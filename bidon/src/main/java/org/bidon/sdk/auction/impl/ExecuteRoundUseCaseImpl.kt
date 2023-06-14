package org.bidon.sdk.auction.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.ConductBiddingAuctionUseCase
import org.bidon.sdk.auction.usecases.ConductNetworkAuctionUseCase
import org.bidon.sdk.auction.usecases.models.ExecuteRoundUseCase
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

internal class ExecuteRoundUseCaseImpl(
    private val adaptersSource: AdaptersSource,
    private val conductBiddingAuction: ConductBiddingAuctionUseCase,
    private val conductNetworkAuction: ConductNetworkAuctionUseCase,
) : ExecuteRoundUseCase {
    override suspend fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        adTypeParam: AdTypeParam,
        round: Round,
        pricefloor: Double,
        lineItems: List<LineItem>,
        onFinish: (remainingLineItems: List<LineItem>) -> Unit,
    ): Result<List<AuctionResult>> = coroutineScope {
        val mutableLineItems = lineItems.toMutableList()
        runCatching {
            val filteredAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in (round.demandIds + round.biddingIds)
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
            val logText = "Round '${round.id}' started with"
            logInfo(Tag, "$logText adapters [${filteredAdapters.joinToString { it.demandId.demandId }}]")
            logInfo(Tag, "$logText line items: $mutableLineItems")
            val adSources = filteredAdapters.getAdSources(demandAd, round, auctionResponse).onEach { adSource ->
                adSource.addAuctionConfigurationId(auctionResponse.auctionConfigurationId ?: 0)
            }
            val roundDeferred = mutableListOf<Deferred<AuctionResult>>()

            // Start Bidding demands auction
            val biddingResultDeferred = if (round.biddingIds.isNotEmpty()) {
                async {
                    conductBiddingAuction.invoke(
                        context = adTypeParam.activity.applicationContext,
                        biddingSources = adSources.filterIsInstance<AdLoadingType.Bidding<AdAuctionParams>>(),
                        participantIds = round.biddingIds,
                        adTypeParam = adTypeParam,
                        demandAd = demandAd,
                        bidfloor = pricefloor,
                        auctionId = auctionResponse.auctionId ?: "",
                        round = round,
                        auctionConfigurationId = auctionResponse.auctionConfigurationId
                    )
                }
            } else {
                null
            }

            // Start Regular AdNetwork demands auction
            if (round.demandIds.isNotEmpty()) {
                val networkResults = conductNetworkAuction.invoke(
                    context = adTypeParam.activity,
                    networkSources = adSources.filterIsInstance<AdLoadingType.Network<AdAuctionParams>>(),
                    participantIds = round.demandIds,
                    adTypeParam = adTypeParam,
                    demandAd = demandAd,
                    lineItems = mutableLineItems,
                    round = round,
                    pricefloor = pricefloor,
                    coroutineScope = this@coroutineScope
                )
                mutableLineItems.clear()
                mutableLineItems.addAll(networkResults.remainingLineItems)
                roundDeferred.addAll(networkResults.results)
            }

            // Collecting results
            val biddingResult = biddingResultDeferred?.await()?.let { listOf(it) }.orEmpty()
            roundDeferred
                .map { deferred ->
                    deferred.await()
                }
                .let { dspResults ->
                    dspResults + biddingResult
                }
                .mapIndexed { index, result ->
                    val details = when (result) {

                        is AuctionResult.Bidding.Success -> {
                            "Bidding ${result.adSource.demandId.demandId}, ${result.adSource.buildBidStatistic()}"
                        }

                        is AuctionResult.Network -> {
                            "DSP ${result.adSource.demandId.demandId}, ${result.adSource.buildBidStatistic()}"
                        }

                        is AuctionResult.Bidding.Failure -> {
                            "Bidding ${result.roundStatus}"
                        }
                    }
                    logInfo(Tag, "Round '${round.id}' result #$index. $details")
                    result
                }.also {
                    onFinish.invoke(mutableLineItems)
                    logInfo(Tag, "Round '${round.id}' finished with ${it.size} results: $it")
                }
        }
    }

    private fun List<Adapter>.getAdSources(
        demandAd: DemandAd,
        round: Round,
        auctionResponse: AuctionResponse
    ) = when (demandAd.adType) {
        AdType.Interstitial -> {
            this.filterIsInstance<AdProvider.Interstitial<AdAuctionParams>>()
                .map {
                    it.interstitial(
                        demandAd = demandAd,
                        roundId = round.id,
                        auctionId = auctionResponse.auctionId ?: ""
                    )
                }
        }

        AdType.Rewarded -> {
            this.filterIsInstance<AdProvider.Rewarded<AdAuctionParams>>().map {
                it.rewarded(
                    demandAd = demandAd,
                    roundId = round.id,
                    auctionId = auctionResponse.auctionId ?: ""
                )
            }
        }

        AdType.Banner -> {
            this.filterIsInstance<AdProvider.Banner<AdAuctionParams>>().map {
                it.banner(
                    demandAd = demandAd,
                    roundId = round.id,
                    auctionId = auctionResponse.auctionId ?: ""
                )
            }
        }
    }
}

private const val Tag = "ExecuteRoundUseCase"
