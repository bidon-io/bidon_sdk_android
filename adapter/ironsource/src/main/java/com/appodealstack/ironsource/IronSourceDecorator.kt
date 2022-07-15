package com.appodealstack.ironsource

import android.app.Activity
import com.appodealstack.ironsource.banner.BNIronSourceBannerLayout
import com.appodealstack.ironsource.banner.BannerImpl
import com.appodealstack.ironsource.banner.IronSourceBannerListener
import com.appodealstack.ironsource.banner.IronSourceLevelPlayBannerListener
import com.appodealstack.ironsource.impl.ISDecoratorInitializerImpl
import com.appodealstack.ironsource.impl.ImpressionsHolder
import com.appodealstack.ironsource.interstitial.InterstitialImpl
import com.appodealstack.ironsource.interstitial.IronSourceInterstitialListener
import com.appodealstack.ironsource.interstitial.IronSourceLevelPlayInterstitialListener
import com.appodealstack.ironsource.rewarded.IronSourceLevelPlayRewardedListener
import com.appodealstack.ironsource.rewarded.IronSourceRewardedListener
import com.appodealstack.ironsource.rewarded.RewardedImpl
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import com.appodealstack.mads.demands.banners.BannerSize
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import com.ironsource.mediationsdk.sdk.InitializationListener
import kotlinx.coroutines.flow.Flow

/**
 * IronSource SDK Decorator
 */
object IronSourceDecorator :
    ISDecorator.Initializer by ISDecoratorInitializerImpl(),
    ISDecorator.Impressions by ImpressionsHolder(),
    ISDecorator.Interstitial by InterstitialImpl(),
    ISDecorator.Rewarded by RewardedImpl(),
    ISDecorator.Banner by BannerImpl()

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
        fun showInterstitial(activity: Activity?, placementName: String? = null)
    }

    interface Rewarded : ISDecorator {
        fun setRewardedVideoListener(rewardedVideoListener: IronSourceRewardedListener)
        fun setLevelPlayRewardedVideoListener(rewardedVideoListener: IronSourceLevelPlayRewardedListener)
        fun removeRewardedVideoListener()
        fun loadRewardedVideo()
        fun showRewardedVideo(placementName: String? = null)
        fun showRewardedVideo(activity: Activity?, placementName: String? = null)
    }

    interface Banner : ISDecorator {
        fun createBanner(activity: Activity, bannerSize: BannerSize): BNIronSourceBannerLayout
        fun loadBanner(ironSourceBannerLayout: BNIronSourceBannerLayout, placementName: String? = null)
        fun destroyBanner(ironSourceBannerLayout: BNIronSourceBannerLayout)

        interface BannerView {
            fun loadAd(placementName: String? = null)
            fun setLevelPlayBannerListener(bannerListener: IronSourceLevelPlayBannerListener)
            fun setBannerListener(bannerListener: IronSourceBannerListener)
            fun removeBannerListener()
            fun destroy()
        }
    }
}