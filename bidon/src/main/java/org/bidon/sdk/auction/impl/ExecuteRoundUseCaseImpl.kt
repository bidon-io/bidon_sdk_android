package org.bidon.sdk.auction.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.ConductBiddingAuctionUseCase
import org.bidon.sdk.auction.usecases.ConductNetworkAuctionUseCase
import org.bidon.sdk.auction.usecases.models.ExecuteRoundUseCase
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.stats.StatisticsCollector

internal class ExecuteRoundUseCaseImpl(
    private val adaptersSource: AdaptersSource,
    private val conductBiddingAuction: ConductBiddingAuctionUseCase,
    private val conductNetworkAuction: ConductNetworkAuctionUseCase,
    private val regulation: Regulation,
) : ExecuteRoundUseCase {
    override suspend fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        adTypeParam: AdTypeParam,
        round: Round,
        pricefloor: Double,
        lineItems: List<LineItem>,
        resultsCollector: ResultsCollector,
        onFinish: (remainingLineItems: List<LineItem>) -> Unit,
    ): Result<List<AuctionResult>> = coroutineScope {
        val mutableLineItems = lineItems.toMutableList()
        runCatching {
            val filteredAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in (round.demandIds + round.biddingIds)
            }
            filteredAdapters.forEach {
                (it as? SupportsRegulation)?.let { supportsRegulation ->
                    logInfo(Tag, "Applying regulation to ${it.demandId.demandId}")
                    supportsRegulation.updateRegulation(regulation)
                }
            }
            val logText = "Round '${round.id}' started with"
            logInfo(Tag, "$logText adapters [${filteredAdapters.joinToString { it.demandId.demandId }}]")
            logInfo(Tag, "$logText line items: $mutableLineItems")
            val adSources = filteredAdapters.getAdSources(demandAd, round, auctionResponse).onEach { adSource ->
                adSource.setStatisticAdType(adTypeParam.asStatisticAdType())
                adSource.addAuctionConfigurationId(auctionResponse.auctionConfigurationId ?: 0)
                adSource.addExternalWinNotificationsEnabled(auctionResponse.externalWinNotificationsEnabled)
            }
            val roundDeferred = mutableListOf<Deferred<AuctionResult>>()

            // Start Bidding demands auction
            val biddingDemands = adSources.filterIsInstance<AdLoadingType.Bidding<AdAuctionParams>>().map {
                (it as AdSource<*>).demandId.demandId
            }
            val biddingResultDeferred = if (biddingDemands.intersect(round.biddingIds.toSet()).isNotEmpty()) {
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
                        auctionConfigurationId = auctionResponse.auctionConfigurationId,
                        resultsCollector = resultsCollector
                    )
                }
            } else {
                null
            }
            logUnknownBiddingAdapters(round, biddingDemands)

            val unknownNetworkDemands = findUnknownNetworkAdapters(round, adSources).onEach {
                resultsCollector.addAuctionResult(it)
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
                    scope = this@coroutineScope,
                    resultsCollector = resultsCollector
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
                }.let {
                    onFinish.invoke(mutableLineItems)
                    logInfo(Tag, "Round '${round.id}' finished with ${it.size} results: $it")
                    it + unknownNetworkDemands
                }
        }
    }

    private fun logUnknownBiddingAdapters(round: Round, biddingDemands: List<String>) {
        (round.biddingIds - biddingDemands.toSet())
            .takeIf { it.isNotEmpty() }
            ?.let {
                logError(
                    tag = Tag,
                    message = "Bidding adapters not found: $it",
                    error = NoSuchElementException(it.joinToString())
                )
            }
    }

    private fun findUnknownNetworkAdapters(
        round: Round,
        adSources: List<AdSource<*>>
    ): List<AuctionResult.Network.UnknownAdapter> {
        return (
            round.demandIds - adSources.filterIsInstance<AdLoadingType.Network<*>>()
                .map { (it as AdSource<*>).demandId.demandId }.toSet()
            )
            .takeIf { it.isNotEmpty() }
            ?.let { unknownDemandIds ->
                logError(
                    tag = Tag,
                    message = "Adapters not found: $unknownDemandIds",
                    error = NoSuchElementException(unknownDemandIds.joinToString())
                )
                unknownDemandIds
            }?.map {
                AuctionResult.Network.UnknownAdapter(
                    adapterName = it
                )
            }.orEmpty()
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

    private fun AdTypeParam.asStatisticAdType(): StatisticsCollector.AdType {
        return when (this) {
            is AdTypeParam.Banner -> {
                StatisticsCollector.AdType.Banner(
                    format = when (bannerFormat) {
                        BannerFormat.Banner -> BannerRequestBody.StatFormat.Banner320x50
                        BannerFormat.LeaderBoard -> BannerRequestBody.StatFormat.LeaderBoard728x90
                        BannerFormat.MRec -> BannerRequestBody.StatFormat.MRec300x250
                        BannerFormat.Adaptive -> BannerRequestBody.StatFormat.AdaptiveBanner320x50
                    }
                )
            }

            is AdTypeParam.Interstitial -> StatisticsCollector.AdType.Interstitial
            is AdTypeParam.Rewarded -> StatisticsCollector.AdType.Rewarded
        }
    }
}

private const val Tag = "ExecuteRoundUseCase"
