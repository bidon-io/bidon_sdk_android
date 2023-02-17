package com.appodealstack.bidon.ads

import com.appodealstack.bidon.config.BidonError

/**
 * Created by Aleksei Cherniaev on 17/02/2023.
 */
interface FullscreenAdListener {
    fun onAdShowFailed(cause: BidonError) {}
    fun onAdClosed(ad: Ad) {}
}