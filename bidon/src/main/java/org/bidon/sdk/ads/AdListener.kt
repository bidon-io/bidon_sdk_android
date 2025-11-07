package org.bidon.sdk.ads

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 06/02/2023.
 */
public interface AdListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     */
    public fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    public fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError)

    public fun onAdShown(ad: Ad) // equals onAdImpression()
    public fun onAdShowFailed(cause: BidonError) {}

    public fun onAdClicked(ad: Ad) {}
    public fun onAdExpired(ad: Ad) {}
}
