package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import com.appodealstack.applovin.*
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.adapters.*
import java.lang.ref.WeakReference

internal class InterstitialAdWrapperImpl(
    private val adUnitId: String,
    activity: Activity
) : InterstitialAdWrapper {

    private val activityRef = WeakReference(activity)

    override val demandAd: DemandAd by lazy {
        DemandAd(
            adType = AdType.Interstitial,
        )
    }

    override val isReady: Boolean
        get() = SdkCore.canShow(demandAd)

    override fun loadAd() {
        SdkCore.loadAd(activityRef.get(), demandAd, bundleOf(AdUnitIdKey to adUnitId))
    }

    override fun getAdUnitId(): String = adUnitId

    override fun showAd(
        placement: String?,
        customData: String?,
        containerView: ViewGroup?,
        lifecycle: Lifecycle?
    ) {
        SdkCore.showAd(
            demandAd = demandAd,
            activity = activityRef.get(),
            adParams = bundleOf(PlacementKey to placement, CustomDataKey to customData),
        )
    }

    override fun setListener(bnInterstitialListener: BNInterstitialListener) {
        SdkCore.setListener(demandAd, bnInterstitialListener.asAdListener())
    }

    override fun setRevenueListener(adRevenueListener: AdRevenueListener) {
        SdkCore.setRevenueListener(demandAd, adRevenueListener)

    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf(AdUnitIdKey to adUnitId))
    }

    override fun setExtraParameter(key: String, value: String?) {
        SdkCore.setExtras(
            demandAd,
            bundleOf(
                KeyKey to key,
                ValueKey to value,
                AdUnitIdKey to adUnitId
            )
        )
    }
}

private fun BNInterstitialListener.asAdListener(): AdListener {
    return object : AdListener {
        override fun onAdLoaded(ad: Ad) {
            this@asAdListener.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: Throwable) {
            this@asAdListener.onAdLoadFailed(cause)
        }

        override fun onAdDisplayed(ad: Ad) {
            this@asAdListener.onAdDisplayed(ad)
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            this@asAdListener.onAdDisplayFailed(cause)
        }

        override fun onAdImpression(ad: Ad) {
        }

        override fun onAdClicked(ad: Ad) {
            this@asAdListener.onAdClicked(ad)
        }

        override fun onAdHidden(ad: Ad) {
            this@asAdListener.onAdHidden(ad)
        }

        override fun onDemandAdLoaded(ad: Ad) {
            this@asAdListener.onDemandAdLoaded(ad)
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            this@asAdListener.onDemandAdLoadFailed(cause)
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            this@asAdListener.onAuctionFinished(ads)
        }
    }
}