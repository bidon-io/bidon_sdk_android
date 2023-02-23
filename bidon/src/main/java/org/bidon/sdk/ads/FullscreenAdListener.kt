package org.bidon.sdk.ads

import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 17/02/2023.
 */
interface FullscreenAdListener {
    fun onAdShowFailed(cause: BidonError) {}
    fun onAdClosed(ad: Ad) {}
}