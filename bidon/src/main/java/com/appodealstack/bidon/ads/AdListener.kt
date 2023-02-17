package com.appodealstack.bidon.ads

import com.appodealstack.bidon.auction.AuctionListener
import com.appodealstack.bidon.config.BidonError

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [AuctionListener.onAuctionSuccess]
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
