package com.appodealstack.bidon.view.helper

import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.view.BannerListener
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

    override fun onAdShowFailed(cause: BidonError) {
        userListener()?.onAdShowFailed(cause)
    }

    override fun onAdShown(ad: Ad) {
        userListener()?.onAdShown(ad)
    }

    override fun onAdClicked(ad: Ad) {
        userListener()?.onAdClicked(ad)
    }

    override fun onAdClosed(ad: Ad) {
        userListener()?.onAdClosed(ad)
    }

    override fun onAdExpired(ad: Ad) {
        userListener()?.onAdExpired(ad)
    }

    override fun auctionStarted() {
        userListener()?.auctionStarted()
    }

    override fun auctionSucceed(auctionResults: List<AuctionResult>) {
        userListener()?.auctionSucceed(auctionResults)
    }

    override fun auctionFailed(error: Throwable) {
        userListener()?.auctionFailed(error)
    }

    override fun roundStarted(roundId: String) {
        userListener()?.roundStarted(roundId)
    }

    override fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {
        userListener()?.roundSucceed(roundId, roundResults)
    }

    override fun roundFailed(roundId: String, error: Throwable) {
        userListener()?.roundFailed(roundId, error)
    }
}