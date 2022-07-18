package com.appodealstack.ironsource.interstitial

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.PlacementKey
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*

internal class InterstitialImpl : ISDecorator.Interstitial {
    private var userListener: IronSourceInterstitialListener? = null
    private var userLevelPlayListener: IronSourceLevelPlayInterstitialListener? = null
    private val demandAd by lazy { DemandAd(AdType.Interstitial) }

    init {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                userListener?.onInterstitialAdReady()
                userLevelPlayListener?.onAdReady(ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                userListener?.onInterstitialAdLoadFailed(cause)
                userLevelPlayListener?.onAdLoadFailed(cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                userListener?.onInterstitialAdShowSucceeded()
                userLevelPlayListener?.onAdShowSucceeded(ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                userListener?.onInterstitialAdShowFailed(cause)
                userLevelPlayListener?.onAdShowFailed(cause)
            }

            override fun onAdImpression(ad: Ad) {
                userListener?.onInterstitialAdOpened()
                userLevelPlayListener?.onAdOpened(ad)
            }

            override fun onAdClicked(ad: Ad) {
                userListener?.onInterstitialAdClicked()
                userLevelPlayListener?.onAdClicked(ad)
            }

            override fun onAdHidden(ad: Ad) {
                userListener?.onInterstitialAdClosed()
                userLevelPlayListener?.onAdClosed(ad)
            }

            override fun onDemandAdLoaded(ad: Ad) {
                userListener?.onDemandAdLoaded(ad)
                userLevelPlayListener?.onDemandAdLoaded(ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                userListener?.onDemandAdLoadFailed(cause)
                userLevelPlayListener?.onDemandAdLoadFailed(cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                userListener?.onAuctionFinished(ads)
                userLevelPlayListener?.onAuctionFinished(ads)
            }
        })
    }

    override fun setInterstitialListener(interstitialListener: IronSourceInterstitialListener) {
        this.userListener = interstitialListener
    }

    override fun setLevelPlayInterstitialListener(levelPlayInterstitialListener: IronSourceLevelPlayInterstitialListener) {
        this.userLevelPlayListener = levelPlayInterstitialListener
    }

    override fun removeInterstitialListener() {
        this.userListener = null
        this.userLevelPlayListener = null
    }

    override fun loadInterstitial() {
        SdkCore.loadAd(activity = null, demandAd = demandAd, adParams = bundleOf())
    }

    override fun showInterstitial(placementName: String?) {
        showInterstitial(IronSourceDecorator.activity, placementName)
    }

    override fun showInterstitial(activity: Activity?, placementName: String?) {
        SdkCore.showAd(
            activity = activity ?: IronSourceDecorator.activity,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementName)
        )
    }
}