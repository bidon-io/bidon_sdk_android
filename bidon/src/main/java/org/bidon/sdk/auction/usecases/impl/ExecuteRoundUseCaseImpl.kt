package org.bidon.sdk.auction.usecases.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.ConductBiddingRoundUseCase
import org.bidon.sdk.auction.usecases.ConductNetworkRoundUseCase
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.BidType

internal class ExecuteRoundUseCaseImpl(
    private val adaptersSource: AdaptersSource,
    private val conductBiddingAuction: ConductBiddingRoundUseCase,
    private val conductNetworkAuction: ConductNetworkRoundUseCase,
    private val regulation: Regulation,
) : ExecuteRoundUseCase {
    override suspend fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        adTypeParam: AdTypeParam,
        round: RoundRequest,
        roundIndex: Int,
        pricefloor: Double,
        lineItems: List<LineItem>,
        resultsCollector: ResultsCollector,
        onFinish: (remainingLineItems: List<LineItem>) -> Unit,
    ): Result<List<AuctionResult>> = coroutineScope {
        val mutableLineItems = lineItems.toMutableList()
        runCatching {
            val logText = "Round '${round.id}' started with"
            logInfo(TAG, "$logText line items: $mutableLineItems")
            val roundDeferred = mutableListOf<Deferred<AuctionResult>>()

            /**
             * Bidding demands auction
             */
            val filteredBiddingAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in round.biddingIds
            }.onEach(::applyRegulation)
            logInfo(TAG, "$logText bidding adapters [${filteredBiddingAdapters.joinToString { it.demandId.demandId }}]")
            val biddingAdSources = filteredBiddingAdapters
                .getAdSources(demandAd.adType)
                .onEach {
                    applyParams(
                        adSource = it,
                        adTypeParam = adTypeParam,
                        auctionResponse = auctionResponse,
                        demandAd = demandAd,
                        round = round,
                        roundIndex = roundIndex,
                        bidType = BidType.RTB
                    )
                }
                .filterIsInstance<Mode.Bidding>()
            // Start Bidding demands auction
            val biddingDemands = biddingAdSources.map {
                (it as AdSource<*>).demandId.demandId
            }
            val biddingResultDeferred = if (biddingDemands.intersect(round.biddingIds.toSet()).isNotEmpty()) {
                async {
                    conductBiddingAuction.invoke(
                        context = adTypeParam.activity.applicationContext,
                        biddingSources = biddingAdSources,
                        participantIds = round.biddingIds,
                        adTypeParam = adTypeParam,
                        demandAd = demandAd,
                        bidfloor = pricefloor,
                        auctionId = auctionResponse.auctionId,
                        round = round,
                        auctionConfigurationId = auctionResponse.auctionConfigurationId,
                        resultsCollector = resultsCollector
                    )
                }
            } else {
                null
            }

            /**
             * Regular AdNetwork demands auction
             */
            val filteredAdNetworkAdapters = adaptersSource.adapters.filter {
                it.demandId.demandId in round.demandIds
            }.onEach(::applyRegulation)
            logInfo(TAG, "$logText network adapters [${filteredAdNetworkAdapters.joinToString { it.demandId.demandId }}]")
            val networkAdSources = filteredAdNetworkAdapters.getAdSources(demandAd.adType)
                .onEach {
                    applyParams(
                        adSource = it,
                        adTypeParam = adTypeParam,
                        auctionResponse = auctionResponse,
                        demandAd = demandAd,
                        round = round,
                        roundIndex = roundIndex,
                        bidType = BidType.CPM
                    )
                }
                .filterIsInstance<Mode.Network>()

            /**
             * Find unknown adapters
             */
            resultsCollector.findUnknownBiddingAdapters(round, biddingAdSources)
            resultsCollector.findUnknownNetworkAdapters(round, networkAdSources)

            // Start Regular AdNetwork demands auction
            if (round.demandIds.isNotEmpty()) {
                val networkResults = conductNetworkAuction.invoke(
                    context = adTypeParam.activity,
                    networkSources = networkAdSources,
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

            /**
             * Wait for results
             */
            biddingResultDeferred?.await()
            roundDeferred.map { deferred -> deferred.await() }

            /**
             * Collecting results
             */
            resultsCollector.getRoundResults()
                .let { roundResult ->
                    (roundResult as? RoundResult.Results)?.let {
                        it.networkResults + (it.biddingResult as? BiddingResult.FilledAd)?.results.orEmpty()
                    }.orEmpty()
                }.mapIndexed { index, result ->
                    val type = "Bidding".takeIf { result is AuctionResult.Bidding } ?: "DSP"
                    val details = "$type ${result.adSource.demandId.demandId}, ${result.adSource.getStats()}"
                    logInfo(TAG, "Round '${round.id}' result #$index. $details")
                    result
                }.let {
                    onFinish.invoke(mutableLineItems)
                    logInfo(TAG, "Round '${round.id}' finished with ${it.size} results: $it")
                    it
                }
        }
    }

    private fun applyParams(
        adSource: AdSource<AdAuctionParams>,
        adTypeParam: AdTypeParam,
        auctionResponse: AuctionResponse,
        demandAd: DemandAd,
        round: RoundRequest,
        roundIndex: Int,
        bidType: BidType
    ) {
        adSource.addRoundInfo(
            auctionId = auctionResponse.auctionId,
            roundId = round.id,
            demandAd = demandAd,
            roundIndex = roundIndex,
            bidType = bidType
        )
        adSource.setStatisticAdType(adTypeParam.asStatisticAdType())
        adSource.addAuctionConfigurationId(auctionResponse.auctionConfigurationId ?: 0)
        adSource.addExternalWinNotificationsEnabled(auctionResponse.externalWinNotificationsEnabled)
    }

    private fun applyRegulation(adapter: Adapter) {
        (adapter as? SupportsRegulation)?.let { supportsRegulation ->
            logInfo(TAG, "Applying regulation to ${adapter.demandId.demandId}")
            supportsRegulation.updateRegulation(regulation)
        }
    }

    private fun ResultsCollector.findUnknownBiddingAdapters(
        round: RoundRequest,
        adSources: List<Mode.Bidding>
    ) {
        (round.biddingIds - adSources.map { (it as AdSource<*>).demandId.demandId }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.also { unknownDemandIds ->
                logError(
                    tag = TAG,
                    message = "Bidding adapters not found: $unknownDemandIds",
                    error = NoSuchElementException(unknownDemandIds.joinToString())
                )
            }?.onEach { adapterName ->
                this.add(AuctionResult.UnknownAdapter(adapterName, AuctionResult.UnknownAdapter.Type.Bidding))
            }
    }

    private fun ResultsCollector.findUnknownNetworkAdapters(
        round: RoundRequest,
        adSources: List<Mode.Network>
    ) {
        (round.demandIds - adSources.map { (it as AdSource<*>).demandId.demandId }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.let { unknownDemandIds ->
                logError(
                    tag = TAG,
                    message = "DSP adapters not found: $unknownDemandIds",
                    error = NoSuchElementException(unknownDemandIds.joinToString())
                )
                unknownDemandIds
            }?.onEach { adapterName ->
                this.add(AuctionResult.UnknownAdapter(adapterName, AuctionResult.UnknownAdapter.Type.Network))
            }
    }

    private fun List<Adapter>.getAdSources(adType: AdType) = when (adType) {
        AdType.Interstitial -> {
            this.filterIsInstance<AdProvider.Interstitial<AdAuctionParams>>().mapNotNull { adapter ->
                kotlin.runCatching {
                    adapter.interstitial().apply { addDemandId((adapter as Adapter).demandId) }
                }.onFailure {
                    logError(TAG, "Failed to create interstitial ad source", it)
                }.getOrNull()
            }
        }

        AdType.Rewarded -> {
            this.filterIsInstance<AdProvider.Rewarded<AdAuctionParams>>().mapNotNull { adapter ->
                kotlin.runCatching {
                    adapter.rewarded().apply { addDemandId((adapter as Adapter).demandId) }
                }.onFailure {
                    logError(TAG, "Failed to create rewarded ad source", it)
                }.getOrNull()
            }
        }

        AdType.Banner -> {
            this.filterIsInstance<AdProvider.Banner<AdAuctionParams>>().mapNotNull { adapter ->
                runCatching {
                    adapter.banner().apply { addDemandId((adapter as Adapter).demandId) }
                }.onFailure {
                    logError(TAG, "Failed to create banner ad source", it)
                }.getOrNull()
            }
        }
    }

    private fun AdTypeParam.asStatisticAdType(): StatisticsCollector.AdType {
        return when (this) {
            is AdTypeParam.Banner -> {
                StatisticsCollector.AdType.Banner(
                    format = when (bannerFormat) {
                        BannerFormat.Banner -> BannerRequest.StatFormat.Banner320x50
                        BannerFormat.LeaderBoard -> BannerRequest.StatFormat.LeaderBoard728x90
                        BannerFormat.MRec -> BannerRequest.StatFormat.MRec300x250
                        BannerFormat.Adaptive -> BannerRequest.StatFormat.AdaptiveBanner320x50
                    }
                )
            }

            is AdTypeParam.Interstitial -> StatisticsCollector.AdType.Interstitial
            is AdTypeParam.Rewarded -> StatisticsCollector.AdType.Rewarded
        }
    }
}

private const val TAG = "ExecuteRoundUseCase"
