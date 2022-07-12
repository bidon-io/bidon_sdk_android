package com.appodeal.mads.ui.listener

import com.appodealstack.fyber.interstitial.FyberInterstitialListener
import com.appodealstack.fyber.rewarded.FyberRewardedListener
import com.appodealstack.mads.demands.Ad

internal fun createFyberInterstitialListener(log: (String) -> Unit): FyberInterstitialListener {
    return object : FyberInterstitialListener {
        override fun onDemandAdLoaded(placementId: String, ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("Interstitial: onDemandAdLoaded($ad)")
        }

        override fun onDemandAdLoadFailed(placementId: String, cause: Throwable) {
            log("onDemandAdLoadFailed: $cause")
            println("Interstitial: onDemandAdLoadFailed($cause)")
        }

        override fun onAuctionFinished(placementId: String, ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onWinnerFound")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
            println("Interstitial: onWinnerFound($ads)")
        }

        override fun onAvailable(placementId: String, ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("Interstitial: onAdLoaded($ad)")
        }

        override fun onShow(placementId: String, ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
            println("Interstitial: onAdDisplayed($ad)")
        }

        override fun onShowFailure(placementId: String, cause: Throwable) {
            log("onAdDisplayFailed: $cause")
            println("Interstitial: onAdDisplayed($cause)")
        }

        override fun onHide(placementId: String, ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
            // Interstitial ad is hidden. Pre-load the next ad
            println("Interstitial: onAdHidden($ad)")
        }

        override fun onClick(placementId: String, ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
            println("Interstitial: onAdClicked($ad)")
        }

        override fun onUnavailable(placementId: String, cause: Throwable) {
            // Interstitial ad failed to load
            println("Interstitial: onAdLoadFailed($cause)")
        }
    }
}

internal fun createFyberRewardedListener(log: (String) -> Unit): FyberRewardedListener {
    return object : FyberRewardedListener {
        override fun onDemandAdLoaded(placementId: String, ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("Rewarded: onDemandAdLoaded($ad)")
        }

        override fun onDemandAdLoadFailed(placementId: String, cause: Throwable) {
            log("onDemandAdLoadFailed: $cause")
            println("Rewarded: onDemandAdLoadFailed($cause)")
        }

        override fun onAuctionFinished(placementId: String, ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onWinnerFound")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
            println("Rewarded: onWinnerFound($ads)")
        }

        override fun onAvailable(placementId: String, ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
            println("Rewarded: onAdLoaded($ad)")
        }

        override fun onShow(placementId: String, ad: Ad) {
            log("onAdDisplayed: ${ad.demandId.demandId}, price=${ad.price}")
            println("Rewarded: onAdDisplayed($ad)")
        }

        override fun onShowFailure(placementId: String, cause: Throwable) {
            log("onAdDisplayFailed: $cause")
            println("Rewarded: onAdDisplayed($cause)")
        }

        override fun onHide(placementId: String, ad: Ad) {
            log("onAdHidden: ${ad.demandId.demandId}, price=${ad.price}")
            // Interstitial ad is hidden. Pre-load the next ad
            println("Rewarded: onAdHidden($ad)")
        }

        override fun onClick(placementId: String, ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
            println("Rewarded: onAdClicked($ad)")
        }

        override fun onUnavailable(placementId: String, cause: Throwable) {
            // Interstitial ad failed to load
            println("Rewarded: onAdLoadFailed($cause)")
        }

        override fun onCompletion(placementId: String, userRewarded: Boolean) {
            log("onCompletion: placementId=$placementId, userRewarded=$userRewarded")
            println("Rewarded: onCompletion($placementId, $userRewarded)")
        }
    }
}