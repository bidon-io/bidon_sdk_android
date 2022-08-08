package com.appodeal.mads.ui.listener

import com.appodealstack.ironsource.banner.IronSourceLevelPlayBannerListener
import com.appodealstack.ironsource.interstitial.IronSourceLevelPlayInterstitialListener
import com.appodealstack.ironsource.rewarded.IronSourceLevelPlayRewardedListener
import com.appodealstack.bidon.demands.Ad
import com.appodealstack.bidon.demands.DemandError
import com.appodealstack.bidon.demands.RewardedAdListener

internal fun createIronSourceInterstitialListener(log: (String) -> Unit): IronSourceLevelPlayInterstitialListener {
    return object : IronSourceLevelPlayInterstitialListener {
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

internal fun createIronSourceRewardedListener(log: (String) -> Unit): IronSourceLevelPlayRewardedListener {
    return object : IronSourceLevelPlayRewardedListener {
        override fun onAdReady(ad: Ad) {
            log("onAdReady: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAdOpened(ad: Ad) {
            log("onAdOpened: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
            log("onAdRewarded: ${ad.demandId.demandId}, price=${ad.price}, reward=$reward")
        }

        override fun onAdShowFailed(cause: Throwable) {
            log("onAdShowFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
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

internal fun createIronSourceBannerListener(log: (String) -> Unit): IronSourceLevelPlayBannerListener {
    return object : IronSourceLevelPlayBannerListener {
        override fun onAdLoaded(ad: Ad) {
            log("onAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLoadFailed(cause: Throwable) {
            log("onAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAdClicked(ad: Ad) {
            log("onAdClicked: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdLeftApplication(ad: Ad) {
            log("onAdLeftApplication: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdScreenPresented(ad: Ad) {
            log("onAdScreenPresented: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onAdScreenDismissed(ad: Ad) {
            log("onAdScreenDismissed: ${ad.demandId.demandId}, price=${ad.price}")
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