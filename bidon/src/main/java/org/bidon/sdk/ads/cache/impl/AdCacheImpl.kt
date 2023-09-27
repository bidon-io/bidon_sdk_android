package org.bidon.sdk.ads.cache.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.Cacheable
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.models.AdCoordinator
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.usecases.LineItemsPortal
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

internal class AdCacheImpl(
    override val demandAd: DemandAd,
    private val scope: CoroutineScope,
    private val pauseResumeObserver: PauseResumeObserver,
    private val resolver: AuctionResolver,
    private val adCoordinator: AdCoordinator
) : AdCache {
    private val Tag = "${TAG}_${demandAd.adType.code}"
    private val isLoading = MutableStateFlow(false)
    private val results = MutableStateFlow(emptyList<AuctionResult>())
    private var job: Job? = null
    private var previousBidStat: BidStat? = null
    private var previousDemandId: String? = null
    private var settings: Cacheable.Settings = Cacheable.DefaultSettings
    private var retryTimeout = settings.minCacheTimeoutMs

    override fun withSettings(settings: Cacheable.Settings) {
        this.settings = settings
    }

    override fun cache(
        adTypeParam: AdTypeParam,
        onEach: (AuctionResult) -> Unit
    ) {
        job?.cancel()
        job = scope.launch {
            results.value = emptyList()
            while (true) {
                results.first { it.size < settings.minCacheSize }
                isLoading.first { !it }
                pauseResumeObserver.lifecycleFlow.first { it == ActivityLifecycleState.Resumed }
                load(adTypeParam, onEach)
                delay(retryTimeout)
                retryTimeout = minOf(retryTimeout * 2, settings.maxCacheTimeoutMs)
            }
        }
    }

    override fun peek(): AuctionResult? = results.value.firstOrNull()

    override suspend fun poll(): AuctionResult {
        var next = results.first { it.isNotEmpty() }.first()
        if (settings.useDifferentDemands && previousDemandId == next.adSource.getStats().demandId.demandId) {
            next = results.value.firstOrNull { it.adSource.getStats().demandId.demandId != previousDemandId } ?: next
        }
        previousDemandId = next.adSource.getStats().demandId.demandId
        previousBidStat = next.adSource.getStats()
        results.update { it - next }
        return next
    }

    override fun clear() {
        job?.cancel()
        job = null
        results.value = emptyList()
    }

    private fun load(adTypeParam: AdTypeParam, onEach: (AuctionResult) -> Unit) {
        logInfo(Tag, "Cache started: ${results.value.asString()}")
        if (results.value.size >= settings.minCacheSize) {
            logInfo(Tag, "Cache has enough ads")
            return
        }
        if (!isLoading.getAndUpdate { true }) {
            logInfo(Tag, "Cache ad: $adTypeParam")
            val existing = results.value.groupBy { auctionResult ->
                val a = auctionResult.adSource.getStats()
                a.demandId
            }.map { (demandId, results) ->
                demandId to results.maxBy { it.adSource.getStats().ecpm }.adSource.getStats()
            }.toMap() + buildMap {
                // Exclude current banner
                if (demandAd.adType == AdType.Banner) {
                    previousBidStat?.let { put(it.demandId, it) }
                }
            }
            existing.forEach { (d, b) ->
                logInfo(Tag, "Existing: $d -> $b")
            }
            adCoordinator.startAuction(existing, adTypeParam.pricefloor)
            val auction: Auction = get()
            auction.start(
                demandAd = demandAd,
                adTypeParamData = adTypeParam.copy(
                    pricefloor = maxOf(adTypeParam.pricefloor, results.value.firstOrNull()?.adSource?.getStats()?.ecpm ?: 0.0)
                ),
                adCoordinator = adCoordinator,
                onSuccess = { _ ->
                    logInfo(Tag, "Auction completed ${results.value.asString()}")
                    isLoading.value = false
                },
                onFailure = {
                    logInfo(Tag, "Auction failed: ${results.value.asString()}")
                    isLoading.value = false
                },
                onEach = { roundResults ->
                    retryTimeout = settings.minCacheTimeoutMs
                    scope.launch {
                        results.update {
                            resolver.sortWinners(it + roundResults).take(settings.cacheCapacity)
                        }
                        onEach(results.value.first())
                        roundResults.forEach { trackExpired(it) }
                        logInfo(Tag, "Round completed: ${results.value.asString()}")
                    }
                }
            )
        } else {
            logInfo(Tag, "Cache is already loading")
        }
    }

    private fun AdTypeParam.copy(pricefloor: Double): AdTypeParam {
        return when (val param = this) {
            is AdTypeParam.Banner -> AdTypeParam.Banner(
                activity = param.activity,
                pricefloor = pricefloor,
                bannerFormat = param.bannerFormat,
                containerWidth = param.containerWidth
            )

            is AdTypeParam.Interstitial -> AdTypeParam.Interstitial(
                activity = param.activity,
                pricefloor = pricefloor,
            )

            is AdTypeParam.Rewarded -> AdTypeParam.Rewarded(
                activity = param.activity,
                pricefloor = pricefloor,
            )
        }
    }

    private fun trackExpired(actionResult: AuctionResult) {
        actionResult.adSource.adEvent.onEach { event ->
            if (event is AdEvent.Expired) {
                results.update { it - actionResult }
            }
        }.launchIn(scope)
    }

    private fun List<AuctionResult>.asString(): String {
        return "(${this.size}) " + this.joinToString { auctionResult ->
            auctionResult.adSource.getStats().let { "${it.demandId.demandId}:${it.ecpm}" }
        }
    }
}