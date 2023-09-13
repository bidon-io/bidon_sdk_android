package org.bidon.sdk.ads.cache.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bidon.sdk.ads.cache.Refresher

internal class RefresherImpl(
    private val dispatcher: CoroutineDispatcher
) : Refresher {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope by lazy { CoroutineScope(dispatcher.limitedParallelism(1)) }
    private val capping = mutableSetOf<String>()
    private val refresh = mutableMapOf<String, Deferred<Unit>>()

    override fun isPlacementEnabled(placementId: String): Boolean {
        return !capping.contains(placementId)
    }

    override fun startCapping(placementId: String, timeoutMs: Long) {
        scope.launch {
            if (!capping.contains(placementId)) {
                synchronized(placementId) {
                    if (capping.contains(placementId)) return@launch
                    capping.add(placementId)
                }
                delay(timeoutMs)
                capping.remove(placementId)
            }
        }
    }

    override fun startRefresh(placementId: String, timeoutMs: Long, onRefresh: () -> Unit) {
        refresh[placementId]?.cancel()
        refresh[placementId] = scope.async {
            delay(timeoutMs)
            refresh.remove(placementId)
            onRefresh()
        }
    }

    override fun cancelAll() {
        capping.clear()
        refresh.values.forEach { it.cancel() }
        refresh.clear()
    }
}