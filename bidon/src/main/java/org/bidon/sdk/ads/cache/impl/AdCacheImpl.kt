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
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.AdCache.Companion.MIN_CACHE_TIMEOUT
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

internal class AdCacheImpl(
    override val demandAd: DemandAd,
    private val cacheItemToStartLoading: Int,
    private val cacheCapacity: Int,
    private val scope: CoroutineScope,
    private val pauseResumeObserver: PauseResumeObserver,
    private val resolver: AuctionResolver,
) : AdCache {
    private val Tag get() = TAG
    private val isLoading = MutableStateFlow(false)
    private val results = MutableStateFlow(emptyList<AuctionResult>())
    private var job: Job? = null

    override fun cache(
        adTypeParam: AdTypeParam,
        onEach: (AuctionResult) -> Unit
    ) {
        job?.cancel()
        job = scope.launch {
            results.value = emptyList()
            while (true) {
                results.first { it.size < cacheItemToStartLoading }
                isLoading.first { !it }
                pauseResumeObserver.lifecycleFlow.first { it == ActivityLifecycleState.Resumed }
                load(adTypeParam, onEach)
                delay(MIN_CACHE_TIMEOUT)
            }
        }
    }

    override fun peek(): AuctionResult? = results.value.firstOrNull()

    override suspend fun poll(): AuctionResult {
        val next = results.first { it.isNotEmpty() }.first()
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
        if (results.value.size >= cacheItemToStartLoading) {
            logInfo(Tag, "Cache has enough ads")
            return
        }
        if (!isLoading.getAndUpdate { true }) {
            logInfo(Tag, "Cache ad: $adTypeParam")
            val auction: Auction = get()
            auction.start(
                demandAd = demandAd,
                adTypeParamData = adTypeParam.copy(
                    pricefloor = maxOf(adTypeParam.pricefloor, results.value.firstOrNull()?.adSource?.getStats()?.ecpm ?: 0.0)
                ),
                onSuccess = { _ ->
                    logInfo(Tag, "Auction completed ${results.value.asString()}")
                    isLoading.value = false
                },
                onFailure = {
                    logInfo(Tag, "Auction failed: ${results.value.asString()}")
                    isLoading.value = false
                },
                onEach = { roundResults ->
                    scope.launch {
                        results.update {
                            resolver.sortWinners(it + roundResults).take(cacheCapacity)
                        }
                        results.value.firstOrNull()?.let(onEach)
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