package org.bidon.sdk.ads.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal interface Refresher {

    fun isPlacementEnabled(placementId: String): Boolean
    fun startCapping(placementId: String, timeoutMs: Long)

    fun startRefresh(placementId: String, timeoutMs: Long, onRefresh: () -> Unit)
}

internal class RefresherImpl(
    private val scope: CoroutineScope
) : Refresher {
    private val capping = mutableSetOf<String>()

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
        TODO("Not yet implemented")
    }
}