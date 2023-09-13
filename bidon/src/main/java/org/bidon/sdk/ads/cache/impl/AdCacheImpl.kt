package org.bidon.sdk.ads.cache.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicBoolean

internal class AdCacheImpl(
    override val demandAd: DemandAd,
    private val scope: CoroutineScope
) : AdCache {
    private val Tag get() = TAG
    private val isLoading = AtomicBoolean(false)

    private val cache = PriorityQueue<AuctionResult>(AdCache.CacheCapacity) { ad1, ad2 ->
        ((ad2.adSource.getStats().ecpm - ad1.adSource.getStats().ecpm) * 1000000).toInt()
    }

    override fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (BidonError) -> Unit,
    ) {
        if (cache.size >= AdCache.CacheItemToStartLoading) {
            logInfo(Tag, "Cache has enough ads, size=${cache.size}")
            return
        }
        val isSuccessInvoked = AtomicBoolean(false)
        if (!isLoading.getAndSet(true)) {
            logInfo(Tag, "Cache ad: $adTypeParam")
            val auction: Auction = get()
            auction.start(
                demandAd = demandAd,
                adTypeParamData = adTypeParam,
                onSuccess = { _ ->
                    isLoading.set(false)
                },
                onFailure = {
                    isLoading.set(false)
                    onFailure((it as? BidonError) ?: BidonError.Unspecified(demandId = null, it))
                },
                onEach = { results ->
                    results.forEach { trackExpired(it) }
                    cache.addAll(results)
                    logInfo(Tag, "Cache size: ${cache.size}")
                    logInfo(Tag, "Items ${cache.joinToString { it.adSource.getStats().ecpm.toString() }}")
                    logInfo(Tag, "Items ${cache.joinToString { it.adSource.getStats().demandId.demandId }}")
                    if (!isSuccessInvoked.getAndSet(true)) {
                        onSuccess(results.first())
                    }
                }
            )
        }
    }

    override fun peek(): AuctionResult? = cache.peek()
    override fun poll(): AuctionResult? = cache.poll()

    override fun clear() {
        cache.clear()
    }

    private fun trackExpired(actionResult: AuctionResult) {
        actionResult.adSource.adEvent.onEach {
            if (it is AdEvent.Expired) {
                cache.remove(actionResult)
            }
        }.launchIn(scope)
    }
}