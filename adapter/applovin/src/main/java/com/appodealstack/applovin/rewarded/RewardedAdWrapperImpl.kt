package com.appodealstack.applovin.rewarded

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import com.appodealstack.applovin.*
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*
import java.lang.ref.WeakReference

internal class RewardedAdWrapperImpl(
    private val adUnitId: String,
    activity: Activity
) : RewardedAdWrapper {

    private val activityRef = WeakReference(activity)

    override val demandAd: DemandAd by lazy {
        DemandAd(
            adType = AdType.Rewarded,
        )
    }

    override val isReady: Boolean
        get() = SdkCore.canShow(demandAd)

    override fun loadAd() {
        SdkCore.loadAd(activityRef.get(), demandAd, bundleOf(adUnitIdKey to adUnitId))
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
            adParams = bundleOf(placementKey to placement, customDataKey to customData),
        )
    }

    override fun setListener(bnRewardedListener: BNRewardedListener) {
        SdkCore.setListener(demandAd, bnRewardedListener.asAdListener())
    }

    override fun setRevenueListener(adRevenueListener: AdRevenueListener) {
        SdkCore.setRevenueListener(demandAd, adRevenueListener)

    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf(adUnitIdKey to adUnitId))
    }

    override fun setExtraParameter(key: String, value: String?) {
        SdkCore.setExtras(
            demandAd,
            bundleOf(
                keyKey to key,
                valueKey to value,
                adUnitIdKey to adUnitId
            )
        )
    }
}

private fun BNRewardedListener.asAdListener(): AdListener {
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

        override fun onRewardedStarted(ad: Ad) {
            this@asAdListener.onRewardedStarted(ad)
        }

        override fun onRewardedCompleted(ad: Ad) {
            this@asAdListener.onRewardedCompleted(ad)
        }

        override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
            this@asAdListener.onUserRewarded(ad, reward)
        }
    }
}