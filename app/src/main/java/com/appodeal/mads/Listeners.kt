package com.appodeal.mads

import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.applovin.interstitial.BNInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.applovin.rewarded.BNMaxRewardedAd
import com.appodealstack.applovin.rewarded.BNRewardedListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.RewardedAdListener

internal fun BNMaxRewardedAd.setRewardedListener(log: (String) -> Unit) {
    this.setListener(object : BNRewardedListener {
        override fun onRewardedStarted(ad: Ad) {
            log("onRewardedStarted: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onRewardedStarted($ad)")
        }

        override fun onRewardedCompleted(ad: Ad) {
            log("onRewardedCompleted: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onRewardedCompleted($ad)")
        }

        override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
            log("onUserRewarded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onUserRewarded($ad, $reward)")
        }

        override fun onAdLoaded(ad: Ad) {
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onAdLoaded($ad)")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed: $cause")
            println("MainActivity Rewarded: onAdLoadFailed($cause)")
        }

        override fun onAdDisplayed(ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onAdDisplayed($ad)")
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            log("onAdDisplayFailed: $cause")
            println("MainActivity Rewarded: onAdDisplayFailed($cause)")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onAdClicked($ad)")
        }

        override fun onAdHidden(ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onAdHidden($ad)")
        }

        override fun onDemandAdLoaded(ad: Ad) {
            super.onDemandAdLoaded(ad)
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Rewarded: onDemandAdLoaded($ad)")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            super.onDemandAdLoadFailed(cause)
            log("onDemandAdLoadFailed: $cause")
            println("MainActivity Rewarded: onDemandAdLoadFailed($cause)")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onWinnerFound")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
            println("MainActivity Rewarded: onWinnerFound($ads)")
        }
    })
}

internal fun BNMaxInterstitialAd.setInterstitialListener(log: (String) -> Unit) {
    this.setListener(object : BNInterstitialListener {
        override fun onDemandAdLoaded(ad: Ad) {
            super.onDemandAdLoaded(ad)
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Interstitial: onDemandAdLoaded($ad)")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            super.onDemandAdLoadFailed(cause)
            log("onDemandAdLoadFailed: $cause")
            println("MainActivity Interstitial: onDemandAdLoadFailed($cause)")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onWinnerFound")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
            println("MainActivity Interstitial: onWinnerFound($ads)")
        }

        override fun onAdLoaded(ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Interstitial: onAdLoaded($ad)")
        }

        override fun onAdDisplayed(ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Interstitial: onAdDisplayed($ad)")
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            log("onAdDisplayFailed: $cause")
            println("MainActivity Interstitial: onAdDisplayed($cause)")
        }

        override fun onAdHidden(ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
            // Interstitial ad is hidden. Pre-load the next ad
            println("MainActivity Interstitial: onAdHidden($ad)")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Interstitial: onAdClicked($ad)")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            // Interstitial ad failed to load
            println("MainActivity Interstitial: onAdLoadFailed($cause)")
        }
    })
}

internal fun BNMaxAdView.setBannerListener(log: (String) -> Unit) {
    val bannerListener = object : BNMaxAdViewAdListener {
        override fun onDemandAdLoaded(ad: Ad) {
            super.onDemandAdLoaded(ad)
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Banner: onDemandAdLoaded($ad)")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            super.onDemandAdLoadFailed(cause)
            log("onDemandAdLoadFailed: $cause")
            println("MainActivity Banner: onDemandAdLoadFailed($cause)")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            super.onAuctionFinished(ads)
            val str = StringBuilder()
            str.appendLine("onWinnerFound")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
            println("MainActivity Banner: onWinnerFound($ads)")
        }

        override fun onAdExpanded(ad: Ad) {
        }

        override fun onAdCollapsed(ad: Ad) {
        }

        override fun onAdLoaded(ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Banner: onAdLoaded($ad)")
        }

        override fun onAdDisplayFailed(error: Throwable) {
            log("onAdDisplayFailed: $error")
            println("MainActivity Banner: onAdDisplayed($error)")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
            println("MainActivity Banner: onAdClicked($ad)")
        }
    }
    this.setListener(bannerListener)
}