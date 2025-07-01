package org.bidon.sdk.ads.cache.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.Cacheable
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

/**
 * Created by Aleksei Cherniaev on 28/09/2023.
 */
internal class AdCacheImpl(
    override val demandAd: DemandAd,
    private val scope: CoroutineScope,
    private val resolver: AuctionResolver,
) : AdCache {

    private val tag = "${TAG}_${demandAd.adType.code}"
    private val isLoading = MutableStateFlow(false)
    private val results = MutableStateFlow(emptyList<AuctionResult>())
    private var previousBidStat: BidStat? = null
    private var previousDemandId: String? = null
    private var settings: Cacheable.Settings = Cacheable.DefaultSettings
    private var auction: Auction? = null

    override fun withSettings(settings: Cacheable.Settings) {
        this.settings = settings
    }

    override fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult, AuctionInfo) -> Unit,
        onFailure: (AuctionInfo?, Throwable) -> Unit,
    ) {
        load(adTypeParam, onSuccess, onFailure)
    }

    override fun peek(): AuctionResult? = results.value.firstOrNull()

    override fun pop(): AuctionResult? {
        return results.getAndUpdate {
            it.drop(1)
        }.firstOrNull()
    }

    override suspend fun poll(): AuctionResult {
        val next = results.first { it.isNotEmpty() }.first()
        previousDemandId = next.adSource.getStats().demandId.demandId
        previousBidStat = next.adSource.getStats()
        results.update { it - next }
        return next
    }

    override fun clear() {
        results.value = emptyList()
        if (isLoading.getAndUpdate { false }) {
            logInfo(tag, "Ad is loading, cancel auction")
            auction?.cancel()
            auction = null
        }
    }

    private fun load(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult, AuctionInfo) -> Unit,
        onFailure: (AuctionInfo?, Throwable) -> Unit,
    ) {
        logInfo(tag, "Cache started: ${results.value.asString()}")
        if (results.value.size >= settings.minCacheSize) {
            logInfo(tag, "Cache has enough ads")
            return
        }
        if (!isLoading.getAndUpdate { true }) {
            logInfo(tag, "Cache ad: $adTypeParam")
            auction = get()
            auction?.start(
                demandAd = demandAd,
                adTypeParam = adTypeParam.copy(
                    pricefloor = maxOf(adTypeParam.pricefloor, results.value.firstOrNull()?.adSource?.getStats()?.price ?: 0.0)
                ),
                onSuccess = { winners, auctionInfo ->
                    scope.launch {
                        results.update {
                            resolver.sortWinners(winners).take(settings.cacheCapacity)
                        }
                        winners.intersect(results.value.toSet()).forEach { trackExpired(it) }
                        logInfo(tag, "Auction completed: ${results.value.asString()}")
                        isLoading.value = false
                        results.value.firstOrNull()?.let { onSuccess.invoke(it, auctionInfo) }
                    }
                },
                onFailure = { auctionInfo, cause ->
                    scope.launch {
                        logInfo(tag, "Auction failed: ${results.value.asString()}")
                        onFailure.invoke(auctionInfo, cause)
                        isLoading.value = false
                    }
                },
            )
        } else {
            logInfo(tag, "Ad is already loading")
        }
    }

    private fun AdTypeParam.copy(pricefloor: Double): AdTypeParam {
        return when (val param = this) {
            is AdTypeParam.Banner -> AdTypeParam.Banner(
                activity = param.activity,
                pricefloor = pricefloor,
                auctionKey = param.auctionKey,
                bannerFormat = param.bannerFormat,
                containerWidth = param.containerWidth,
            )

            is AdTypeParam.Interstitial -> AdTypeParam.Interstitial(
                activity = param.activity,
                pricefloor = pricefloor,
                auctionKey = param.auctionKey,
            )

            is AdTypeParam.Rewarded -> AdTypeParam.Rewarded(
                activity = param.activity,
                pricefloor = pricefloor,
                auctionKey = param.auctionKey,
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
            auctionResult.adSource.getStats().let { "${it.demandId.demandId}:${it.price}" }
        }
    }
}