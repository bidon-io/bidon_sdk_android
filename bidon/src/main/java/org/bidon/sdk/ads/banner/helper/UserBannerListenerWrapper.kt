package org.bidon.sdk.ads.banner.helper

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal fun wrapUserBannerListener(userListener: () -> BannerListener?) = object : BannerListener {
    override fun onAdLoaded(ad: Ad) {
        userListener()?.onAdLoaded(ad)
    }

    override fun onAdLoadFailed(cause: BidonError) {
        userListener()?.onAdLoadFailed(cause)
    }

    override fun onAdShown(ad: Ad) {
        userListener()?.onAdShown(ad)
    }

    override fun onAdClicked(ad: Ad) {
        userListener()?.onAdClicked(ad)
    }

    override fun onAdExpired(ad: Ad) {
        userListener()?.onAdExpired(ad)
    }

    override fun onAuctionStarted() {
        userListener()?.onAuctionStarted()
    }

    override fun onAuctionSuccess(auctionResults: List<AuctionResult>) {
        userListener()?.onAuctionSuccess(auctionResults)
    }

    override fun onAuctionFailed(error: Throwable) {
        userListener()?.onAuctionFailed(error)
    }

    override fun onRoundStarted(roundId: String, pricefloor: Double) {
        userListener()?.onRoundStarted(roundId, pricefloor)
    }

    override fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {
        userListener()?.onRoundSucceed(roundId, roundResults)
    }

    override fun onRoundFailed(roundId: String, error: Throwable) {
        userListener()?.onRoundFailed(roundId, error)
    }

    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
        userListener()?.onRevenuePaid(ad, adValue)
    }
}