package org.bidon.sdk.ads

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 06/02/2023.
 */
interface AdListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     */
    fun onAdLoaded(ad: Ad)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(cause: BidonError)

    fun onAdShown(ad: Ad) // equals onAdImpression()
    fun onAdShowFailed(cause: BidonError) {}

    fun onAdClicked(ad: Ad) {}
    fun onAdExpired(ad: Ad) {}
}
