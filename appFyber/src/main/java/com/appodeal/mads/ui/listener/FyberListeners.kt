package com.appodeal.mads.ui.listener

import com.appodealstack.fyber.banner.FyberBannerListener
import com.appodealstack.fyber.interstitial.FyberInterstitialListener
import com.appodealstack.fyber.rewarded.FyberRewardedListener
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.DemandError

internal fun createFyberInterstitialListener(log: (String) -> Unit): FyberInterstitialListener {
    return object : FyberInterstitialListener {
        override fun onDemandAdLoaded(placementId: String, ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(placementId: String, cause: Throwable) {
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(placementId: String, ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }

        override fun onAvailable(placementId: String, ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAvailable: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onShow(placementId: String, ad: Ad) {
            log("onShow: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onShowFailure(placementId: String, cause: Throwable) {
            log("onShowFailure: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onHide(placementId: String, ad: Ad) {
            log("onHide: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onClick(placementId: String, ad: Ad) {
            log("onClick: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onUnavailable(placementId: String, cause: Throwable) {
            // Interstitial ad failed to load
            log("onUnavailable: ${cause::class.java.simpleName}")
        }
    }
}
internal fun createFyberBannerListener(log: (String) -> Unit): FyberBannerListener {
    return object : FyberBannerListener {
        override fun onDemandAdLoaded(placementId: String, ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(placementId: String, cause: Throwable) {
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(placementId: String, ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }

        override fun onError(placementId: String, cause: Throwable) {
            log("onError: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onLoad(placementId: String, ad: Ad) {
            log("onLoad: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onShow(placementId: String, ad: Ad) {
            log("onShow: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onClick(placementId: String, ad: Ad) {
            log("onClick: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onRequestStart(placementId: String, ad: Ad) {
            log("onRequestStart: ${ad.demandId.demandId}, price=${ad.price}")
        }
    }
}

internal fun createFyberRewardedListener(log: (String) -> Unit): FyberRewardedListener {
    return object : FyberRewardedListener {
        override fun onDemandAdLoaded(placementId: String, ad: Ad) {
            log("onDemandAdLoaded: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onDemandAdLoadFailed(placementId: String, cause: Throwable) {
            log("onDemandAdLoadFailed: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onAuctionFinished(placementId: String, ads: List<Ad>) {
            val str = StringBuilder()
            str.appendLine("onAuctionFinished")
            ads.forEachIndexed { i, ad ->
                str.appendLine("#${i + 1} > ${ad.demandId.demandId}, price=${ad.price}")
            }
            log(str.toString())
        }

        override fun onAvailable(placementId: String, ad: Ad) {
            // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
            log("onAvailable: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onShow(placementId: String, ad: Ad) {
            logInternal("++++",">>>>>>>> ")
            log("onShow: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onShowFailure(placementId: String, cause: Throwable) {
            log("onShowFailure: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onHide(placementId: String, ad: Ad) {
            log("onHide: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onClick(placementId: String, ad: Ad) {
            log("onClick: ${ad.demandId.demandId}, price=${ad.price}")
        }

        override fun onUnavailable(placementId: String, cause: Throwable) {
            // Interstitial ad failed to load
            log("onUnavailable: ${(cause as? DemandError)?.demandId?.demandId} ${cause::class.java.simpleName}")
        }

        override fun onCompletion(placementId: String, userRewarded: Boolean) {
            log("onCompletion: placementId=$placementId, userRewarded=$userRewarded")
        }
    }
}