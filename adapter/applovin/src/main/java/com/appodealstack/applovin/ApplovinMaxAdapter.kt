package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.impl.asBidonError
import com.appodealstack.applovin.impl.setCoreListener
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.banners.BannerAutoRefreshProvider
import com.appodealstack.mads.demands.banners.BannerAutoRefreshSource
import com.appodealstack.mads.demands.banners.BannerSize
import com.appodealstack.mads.demands.banners.BannerSizeKey
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter.Mediation,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner,
    BannerAutoRefreshSource,
    PlacementSource by PlacementSourceImpl(),
    AdRevenueSource,
    ExtrasSource by ExtrasSourceImpl() {
    private val adRevenueListeners = mutableMapOf<DemandAd, AdRevenueListener>()
    private var autoRefresh: Boolean = true
    private lateinit var context: Context

    override val demandId: DemandId = ApplovinMaxDemandId

    override suspend fun init(context: Context, configParams: Bundle) {
        require(AppLovinSdk.getInstance(context).isInitialized)
        this.context = context.applicationContext
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        val adUnitId = adParams.getString(AdUnitIdKey)
        val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
            BannerSize.values()[it]
        }
        val maxAdView = MaxAdView(adUnitId, bannerSize.asMaxAdFormat(), context).apply {
            val isAdaptive = getExtras(demandAd)?.getString(AdaptiveBannerKey, "-")
            if (isAdaptive == "true") {
                val width = ViewGroup.LayoutParams.MATCH_PARENT
                // Height calculating should be done on application side and passed with extras[AdaptiveBannerHeightKey]
                val heightPx = adParams.getInt(AdaptiveBannerHeightKey, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams = FrameLayout.LayoutParams(width, heightPx)
            }
            getExtras(demandAd)?.let { bundle ->
                bundle.keySet().forEach { key ->
                    if (bundle.get(key) is String) {
                        setExtraParameter(key, bundle.getString(key))
                    }
                }
            }
            getPlacement(demandAd)?.let {
                placement = it
            }
        }
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxAdView.setListener(object : MaxAdViewAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = Ad(
                                demandId = ApplovinMaxDemandId,
                                demandAd = demandAd,
                                price = maxAd.revenue,
                                sourceAd = maxAd
                            )
                            val auctionResult = AuctionResult(
                                ad = ad,
                                adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider, PlacementProvider,
                                    AdViewProvider, BannerAutoRefreshProvider {
                                    override fun canShow(): Boolean = true
                                    override fun showAd(activity: Activity?, adParams: Bundle) {}
                                    override fun destroy() = maxAdView.destroy()
                                    override fun getAdView(): View = maxAdView

                                    override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
                                        maxAdView.setRevenueListener {
                                            adRevenueListener.onAdRevenuePaid(ad)
                                        }
                                    }

                                    override fun setExtras(adParams: Bundle) {
                                        adParams.keySet().forEach { key ->
                                            if (adParams.get(key) is String) {
                                                maxAdView.setExtraParameter(key, adParams.getString(key))
                                            }
                                        }
                                    }

                                    override fun setAutoRefresh(autoRefresh: Boolean) {
                                        if (autoRefresh) {
                                            maxAdView.startAutoRefresh()
                                        } else {
                                            maxAdView.stopAutoRefresh()
                                        }
                                    }

                                    override fun setPlacement(placement: String?) {
                                        maxAdView.placement = placement
                                    }

                                    override fun getPlacement(): String? = maxAdView.placement
                                }
                            )
                            adRevenueListeners[demandAd]?.let { adRevenueListener ->
                                maxAdView.setRevenueListener {
                                    adRevenueListener.onAdRevenuePaid(ad)
                                }
                            }
                            maxAdView.setCoreListener(auctionResult)
                            continuation.resume(Result.success(auctionResult))
                        }
                    }

                    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                        if (!isFinished.getAndSet(true)) {
                            // remove listener
                            maxAdView.setListener(null)
                            continuation.resume(Result.failure(error.asBidonError()))
                        }
                    }

                    override fun onAdDisplayed(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdHidden(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdClicked(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdExpanded(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdCollapsed(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                })
                maxAdView.loadAd()
            }
        }
    }

    override fun setAutoRefresh(autoRefresh: Boolean) {
        this.autoRefresh = autoRefresh
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity) }
        val adUnitId = adParams.getString(AdUnitIdKey)
        val maxInterstitialAd = MaxInterstitialAd(adUnitId, activity)

        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxInterstitialAd.setListener(
                    object : MaxAdListener {
                        override fun onAdLoaded(maxAd: MaxAd) {
                            if (!isFinished.getAndSet(true)) {
                                val ad = Ad(
                                    demandId = ApplovinMaxDemandId,
                                    demandAd = demandAd,
                                    price = maxAd.revenue,
                                    sourceAd = maxAd
                                )
                                val auctionResult = AuctionResult(
                                    ad = ad,
                                    adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider {
                                        override fun canShow(): Boolean {
                                            return maxInterstitialAd.isReady
                                        }

                                        override fun showAd(activity: Activity?, adParams: Bundle) {
                                            val placement = adParams.getString(PlacementKey)
                                            val customData = adParams.getString(CustomDataKey)
                                            maxInterstitialAd.showAd(placement, customData)
                                        }

                                        override fun destroy() = maxInterstitialAd.destroy()

                                        override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
                                            maxInterstitialAd.setRevenueListener {
                                                adRevenueListener.onAdRevenuePaid(ad)
                                            }
                                        }

                                        override fun setExtras(adParams: Bundle) {
                                            adParams.keySet().forEach { key ->
                                                if (adParams.get(key) is String) {
                                                    maxInterstitialAd.setExtraParameter(key, adParams.getString(key))
                                                }
                                            }
                                        }

                                    }
                                )
                                adRevenueListeners[demandAd]?.let { adRevenueListener ->
                                    maxInterstitialAd.setRevenueListener {
                                        adRevenueListener.onAdRevenuePaid(ad)
                                    }
                                }
                                getExtras(demandAd)?.let { bundle ->
                                    bundle.keySet().forEach { key ->
                                        if (bundle.get(key) is String) {
                                            maxInterstitialAd.setExtraParameter(key, bundle.getString(key))
                                        }
                                    }
                                }
                                maxInterstitialAd.setCoreListener(auctionResult)
                                continuation.resume(Result.success(auctionResult))
                            }
                        }

                        override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                maxInterstitialAd.setListener(null)
                                continuation.resume(Result.failure(error.asBidonError()))
                            }
                        }

                        override fun onAdDisplayed(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdHidden(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                            error("unexpected state. remove on release a28.")
                        }
                    }
                )
                maxInterstitialAd.loadAd()
            }
        }
    }

    override fun setAdRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        adRevenueListeners[demandAd] = adRevenueListener
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity) }
        val adUnitId = adParams.getString(AdUnitIdKey)
        val rewardedAd = MaxRewardedAd.getInstance(adUnitId, activity)
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                rewardedAd.setListener(object : MaxRewardedAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = Ad(
                                demandId = ApplovinMaxDemandId,
                                demandAd = demandAd,
                                price = maxAd.revenue,
                                sourceAd = maxAd
                            )
                            val auctionResult = AuctionResult(
                                ad = ad,
                                adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider {
                                    override fun canShow(): Boolean = rewardedAd.isReady

                                    override fun showAd(activity: Activity?, adParams: Bundle) {
                                        val placement = adParams.getString(PlacementKey)
                                        val customData = adParams.getString(CustomDataKey)
                                        rewardedAd.showAd(placement, customData)
                                    }

                                    override fun destroy() = rewardedAd.destroy()

                                    override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
                                        rewardedAd.setRevenueListener {
                                            adRevenueListener.onAdRevenuePaid(ad)
                                        }
                                    }

                                    override fun setExtras(adParams: Bundle) {
                                        adParams.keySet().forEach { key ->
                                            if (adParams.get(key) is String) {
                                                rewardedAd.setExtraParameter(key, adParams.getString(key))
                                            }
                                        }
                                    }

                                }
                            )
                            adRevenueListeners[demandAd]?.let { adRevenueListener ->
                                rewardedAd.setRevenueListener {
                                    adRevenueListener.onAdRevenuePaid(ad)
                                }
                            }
                            getExtras(demandAd)?.let { bundle ->
                                bundle.keySet().forEach { key ->
                                    if (bundle.get(key) is String) {
                                        rewardedAd.setExtraParameter(key, bundle.getString(key))
                                    }
                                }
                            }
                            rewardedAd.setCoreListener(auctionResult)
                            continuation.resume(Result.success(auctionResult))
                        }

                    }

                    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                        if (!isFinished.getAndSet(true)) {
                            // remove listener
                            rewardedAd.setListener(null)
                            continuation.resume(Result.failure(error.asBidonError()))
                        }
                    }

                    override fun onAdDisplayed(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdHidden(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdClicked(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onRewardedVideoStarted(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onRewardedVideoCompleted(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                        error("unexpected state. remove on release a28.")
                    }
                })
                rewardedAd.loadAd()
            }
        }
    }

    private fun BannerSize.asMaxAdFormat() = when (this) {
        BannerSize.Banner -> MaxAdFormat.BANNER
        BannerSize.LeaderBoard -> MaxAdFormat.LEADER
        BannerSize.MRec -> MaxAdFormat.MREC
    }

}

internal const val AdUnitIdKey = "adUnitId"
internal const val PlacementKey = "placement"
internal const val CustomDataKey = "customData"
internal const val KeyKey = "key"
internal const val ValueKey = "valueKey"
internal const val AdaptiveBannerKey = "adaptive_banner"
internal const val AdaptiveBannerHeightKey = "AdaptiveBannerHeightKey"