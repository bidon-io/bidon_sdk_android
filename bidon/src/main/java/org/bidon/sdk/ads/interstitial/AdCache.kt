package org.bidon.sdk.ads.interstitial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.interstitial.AdCache.Companion.CacheCapacity
import org.bidon.sdk.ads.interstitial.AdCache.Companion.CacheItemToStartLoading
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.util.PriorityQueue

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
internal interface AdCache {
    fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (BidonError) -> Unit,
    )

    fun peek(): AuctionResult?
    fun poll(): AuctionResult?

    fun clear()

    companion object {
        const val CacheItemToStartLoading = 3
        const val CacheCapacity = 6
    }
}

internal class AdCacheImpl(
    private val demandAd: DemandAd,
    private val scope: CoroutineScope
) : AdCache {
    private val Tag get() = TAG

    private val cache = PriorityQueue<AuctionResult>(CacheCapacity) { ad1, ad2 ->
        ((ad2.adSource.getStats().ecpm - ad1.adSource.getStats().ecpm) * 1000000).toInt()
    }

    override fun cache(
        adTypeParam: AdTypeParam,
        onSuccess: (AuctionResult) -> Unit,
        onFailure: (BidonError) -> Unit,
    ) {
        if (cache.size >= CacheItemToStartLoading) {
            logInfo(Tag, "Cache has enough ads, size=${cache.size}")
            return
        }
        logInfo(Tag, "Cache ad: $adTypeParam")
        val auction: Auction = get()
        auction.start(
            demandAd = demandAd,
            adTypeParamData = adTypeParam,
            onSuccess = { results ->
                results.forEach { trackExpired(it) }
                cache.addAll(results)
                logInfo(Tag, "Cache size: ${cache.size}")
                logInfo(Tag, "Items ${cache.joinToString { it.adSource.getStats().ecpm.toString() }}")
                logInfo(Tag, "Items ${cache.joinToString { it.adSource.getStats().demandId.demandId }}")
                onSuccess(results.first())
            },
            onFailure = {
                onFailure((it as? BidonError) ?: BidonError.Unspecified(demandId = null, it))
            }
        )
    }

    override fun peek(): AuctionResult? {
        return cache.peek()
    }

    override fun poll(): AuctionResult? {
        return cache.poll()
    }

    private fun trackExpired(actionResult: AuctionResult) {
        actionResult.adSource.adEvent.onEach {
            if (it is AdEvent.Expired) {
                cache.remove(actionResult)
            }
        }.launchIn(scope)
    }

    override fun clear() {
        cache.clear()
    }
}