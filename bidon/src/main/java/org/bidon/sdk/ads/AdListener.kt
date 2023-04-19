package org.bidon.sdk.ads

import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
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
    fun onAdClicked(ad: Ad) {}
    fun onAdExpired(ad: Ad) {}
}
