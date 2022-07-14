package com.appodealstack.ironsource

import android.app.Activity
import com.appodealstack.ironsource.impl.ISDecoratorInitializerImpl
import com.appodealstack.ironsource.impl.ImpressionsHolder
import com.appodealstack.ironsource.interstitial.BNIronSourceInterstitialListener
import com.appodealstack.ironsource.interstitial.BNIronSourceLevelPlayInterstitialListener
import com.appodealstack.ironsource.interstitial.ISInterstitialImpl
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
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
    ISDecorator.Interstitial by ISInterstitialImpl()

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
        fun setInterstitialListener(interstitialListener: BNIronSourceInterstitialListener)
        fun setLevelPlayInterstitialListener(levelPlayInterstitialListener: BNIronSourceLevelPlayInterstitialListener)
        fun removeInterstitialListener()
        fun loadInterstitial()
        fun showInterstitial(placementName: String? = null)
    }

    interface Rewarded : ISDecorator {
        fun setRewardedVideoListener(rewardedVideoListener: RewardedVideoListener)
        fun removeRewardedVideoListener()
        fun loadRewardedVideo()
        fun showRewardedVideo(placementName: String?)
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

