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
import com.appodealstack.bidon.analytics.Analytic
import com.appodealstack.bidon.analytics.AnalyticParameters
import com.appodealstack.bidon.auctions.AuctionResolver
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters
import com.appodealstack.bidon.demands.banners.BannerSize
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
        val activity: Activity?

        fun register(
            adapterClass: Class<out Analytic<*>>,
            parameters: AnalyticParameters
        ): Initializer

        fun register(
            adapterClass: Class<out Adapter<*>>,
            parameters: AdapterParameters
        ): Initializer

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

        fun hasInterstitial(): Boolean
        fun setInterstitialAuctionResolver(auctionResolver: AuctionResolver)
    }

    interface Rewarded : ISDecorator {
        fun setRewardedVideoListener(rewardedVideoListener: IronSourceRewardedListener)
        fun setLevelPlayRewardedVideoListener(rewardedVideoListener: IronSourceLevelPlayRewardedListener)
        fun removeRewardedVideoListener()
        fun loadRewardedVideo()
        fun showRewardedVideo(placementName: String? = null)
        fun showRewardedVideo(activity: Activity?, placementName: String? = null)

        fun hasRewardedVideo(): Boolean
        fun setRewardedVideoAuctionResolver(auctionResolver: AuctionResolver)
    }

    interface Banner : ISDecorator {
        fun createBanner(activity: Activity, bannerSize: BannerSize): BNIronSourceBannerLayout
        fun loadBanner(ironSourceBannerLayout: BNIronSourceBannerLayout, placementName: String? = null)
        fun destroyBanner(ironSourceBannerLayout: BNIronSourceBannerLayout)
        fun startAutoRefresh(ironSourceBannerLayout: BNIronSourceBannerLayout)
        fun stopAutoRefresh(ironSourceBannerLayout: BNIronSourceBannerLayout)
        fun setAutoRefreshTimeout(ironSourceBannerLayout: BNIronSourceBannerLayout, timeoutMs: Long)
        fun setBannerAuctionResolver(ironSourceBannerLayout: BNIronSourceBannerLayout, auctionResolver: AuctionResolver)

        interface BannerView {
            fun loadAd(placementName: String? = null)
            fun setLevelPlayBannerListener(bannerListener: IronSourceLevelPlayBannerListener)
            fun setBannerListener(bannerListener: IronSourceBannerListener)
            fun removeBannerListener()
            fun destroy()
            fun startAutoRefresh()
            fun stopAutoRefresh()
            fun setAutoRefreshTimeout(timeoutMs: Long)
            fun setAuctionResolver(auctionResolver: AuctionResolver)
        }
    }
}