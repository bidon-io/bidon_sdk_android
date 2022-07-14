package com.appodeal.mads.ui.listener

import com.appodealstack.ironsource.interstitial.BNIronSourceLevelPlayInterstitialListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.DemandError

internal fun createIronSourceInterstitialListener(log: (String) -> Unit): BNIronSourceLevelPlayInterstitialListener {
    return object : BNIronSourceLevelPlayInterstitialListener {
        override fun onAdReady(ad: Ad) {
            log("onAdReady: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAdOpened(ad: Ad) {
            log("onAdOpened: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdShowSucceeded(ad: Ad) {
            log("onAdShowSucceeded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdShowFailed(cause: Throwable) {
            log("onAdShowFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdClosed(ad: Ad) {
            log("onAdClosed: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoaded(ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }
    }
}