package org.bidon.sdk.ads.cache

import androidx.annotation.Keep

/**
 * Created by Aleksei Cherniaev on 13/09/2023.
 */
@Keep
interface Refreshable {
    /**
     * 0 - no refresh
     */
    fun setRefreshTimeout(timeoutMs: Long)

    companion object {
        const val DefaultRefreshTimeout = 5000L
    }
}