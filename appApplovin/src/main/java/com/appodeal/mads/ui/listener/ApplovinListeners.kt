package com.appodeal.mads

import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.applovin.interstitial.BNInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.applovin.rewarded.BNMaxRewardedAd
import com.appodealstack.applovin.rewarded.BNRewardedListener
import com.appodealstack.bidon.demands.Ad
import com.appodealstack.bidon.demands.DemandError
import com.appodealstack.bidon.demands.RewardedAdListener

internal fun BNMaxRewardedAd.setRewardedListener(log: (String) -> Unit) {
    this.setListener(object : BNRewardedListener {
        override fun onRewardedStarted(ad: Ad) {
            log("onRewardedStarted: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onRewardedCompleted(ad: Ad) {
            log("onRewardedCompleted: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
            log("onUserRewarded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoaded(ad: Ad) {
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAdDisplayed(ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            log("onAdDisplayFailed: $cause")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdHidden(ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoaded(ad: Ad) {
            super.onDemandAdLoaded(ad)
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            log("onDemandAdLoadFailed:${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }
    })
}

internal fun BNMaxInterstitialAd.setInterstitialListener(log: (String) -> Unit) {
    this.setListener(object : BNInterstitialListener {
        override fun onDemandAdLoaded(ad: Ad) {
            super.onDemandAdLoaded(ad)
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            super.onDemandAdLoadFailed(cause)
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }

        override fun onAdLoaded(ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdDisplayed(ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            log("onAdDisplayFailed: ${(cause as? DemandError)?.demandId?.demandId} $cause")
        }

        override fun onAdHidden(ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed:${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }
    })
}

internal fun BNMaxAdView.setBannerListener(log: (String) -> Unit) {
    val bannerListener = object : BNMaxAdViewAdListener {
        override fun onDemandAdLoaded(ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }

        override fun onAdExpanded(ad: Ad) {
        }

        override fun onAdCollapsed(ad: Ad) {
        }

        override fun onAdLoaded(ad: Ad) {
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdDisplayFailed(error: Throwable) {
            log("onAdDisplayFailed: ${(error as? DemandError)?.demandId?.demandId} ${error::class.java.simpleName}")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }
    }
    this.setListener(bannerListener)
}