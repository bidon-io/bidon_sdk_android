package com.appodealstack.ironsource

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.ironsource.impl.ISDecoratorInitializerImpl
import com.appodealstack.ironsource.impl.ImpressionsHolder
import com.appodealstack.ironsource.interstitial.IronSourceInterstitialListener
import com.appodealstack.ironsource.interstitial.IronSourceLevelPlayInterstitialListener
import com.appodealstack.ironsource.interstitial.InterstitialImpl
import com.appodealstack.ironsource.rewarded.IronSourceLevelPlayRewardedListener
import com.appodealstack.ironsource.rewarded.IronSourceRewardedListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import com.ironsource.mediationsdk.sdk.*
import kotlinx.coroutines.flow.Flow

/**
 * IronSource SDK Decorator
 */
object IronSourceDecorator :
    ISDecorator.Initializer by ISDecoratorInitializerImpl(),
    ISDecorator.Impressions by ImpressionsHolder(),
    ISDecorator.Interstitial by InterstitialImpl(),
    ISDecorator.Rewarded by RewardedImpl()

sealed interface ISDecorator {
    interface Initializer : ISDecorator {
        fun register(adapterClass: Class<out Adapter<*>>, parameters: AdapterParameters): Initializer

        fun init(
            activity: Activity,
            appKey: String,
            listener: InitializationListener,
            adUnit: IronSource.AD_UNIT? = null
        )
    }

    interface Impressions : ISDecorator {
        fun addImpressionDataListener(impressionDataListener: ImpressionDataListener)
        fun observeImpressions(adUnitId: String): Flow<ImpressionData>
    }

    interface Interstitial : ISDecorator {
        fun setInterstitialListener(interstitialListener: IronSourceInterstitialListener)
        fun setLevelPlayInterstitialListener(levelPlayInterstitialListener: IronSourceLevelPlayInterstitialListener)
        fun removeInterstitialListener()
        fun loadInterstitial()
        fun showInterstitial(placementName: String? = null)
    }

    interface Rewarded : ISDecorator {
        fun setRewardedVideoListener(rewardedVideoListener: IronSourceRewardedListener)
        fun setLevelPlayRewardedVideoListener(rewardedVideoListener: IronSourceLevelPlayRewardedListener)
        fun removeRewardedVideoListener()
        fun loadRewardedVideo()
        fun showRewardedVideo(placementName: String? = null)
    }

    interface Banner : ISDecorator {
        fun createBanner(activity: Activity, bannerSize: ISBannerSize): IronSourceBannerLayout
        fun setBannerListener(bannerListener: BannerListener)
        fun loadBanner(ironSourceBannerLayout: IronSourceBannerLayout)
        fun destroyBanner(ironSourceBannerLayout: IronSourceBannerLayout)
    }
}

object ISBannerSizeDecorator {
    fun setAdaptive(adaptive: Boolean) {
    }
}


internal class RewardedImpl : ISDecorator.Rewarded {
    private var userListener: IronSourceRewardedListener? = null
    private var userLevelPlayListener: IronSourceLevelPlayRewardedListener? = null
    private val demandAd by lazy { DemandAd(AdType.Rewarded) }

    init {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                userLevelPlayListener?.onAdReady(ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                userLevelPlayListener?.onAdLoadFailed(cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                userListener?.onRewardedVideoAdOpened()
                userLevelPlayListener?.onAdOpened(ad)
            }

            override fun onAdDisplayFailed(cause: Throwable) {
                userListener?.onRewardedVideoAdShowFailed(cause)
                userLevelPlayListener?.onAdShowFailed(cause)
            }

            override fun onAdImpression(ad: Ad) {
            }

            override fun onAdClicked(ad: Ad) {
                userListener?.onRewardedVideoAdClicked()
                userLevelPlayListener?.onAdClicked(ad)
            }

            override fun onAdHidden(ad: Ad) {
                userListener?.onRewardedVideoAdClosed()
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

            override fun onRewardedStarted(ad: Ad) {
                userListener?.onRewardedVideoAdStarted()
            }

            override fun onRewardedCompleted(ad: Ad) {
                userListener?.onRewardedVideoAdEnded()
            }

            override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
                userListener?.onRewardedVideoAdRewarded(reward)
                userLevelPlayListener?.onAdRewarded(ad, reward)
            }
        })
    }

    override fun setRewardedVideoListener(rewardedVideoListener: IronSourceRewardedListener) {
        this.userListener = rewardedVideoListener
    }

    override fun setLevelPlayRewardedVideoListener(rewardedVideoListener: IronSourceLevelPlayRewardedListener) {
        this.userLevelPlayListener = rewardedVideoListener
    }

    override fun removeRewardedVideoListener() {
        this.userListener = null
        this.userLevelPlayListener = null
    }

    override fun loadRewardedVideo() {
        SdkCore.loadAd(activity = null, demandAd = demandAd, adParams = bundleOf())
    }

    override fun showRewardedVideo(placementName: String?) {
        SdkCore.showAd(
            activity = null,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementName)
        )
    }


}
