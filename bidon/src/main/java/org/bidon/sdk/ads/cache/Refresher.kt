package org.bidon.sdk.ads.cache

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal interface Refresher {

    fun isPlacementEnabled(placementId: String): Boolean
    fun startCapping(placementId: String, timeoutMs: Long)

    fun startRefresh(placementId: String, timeoutMs: Long, onRefresh: () -> Unit)

    fun cancelAll()
}
