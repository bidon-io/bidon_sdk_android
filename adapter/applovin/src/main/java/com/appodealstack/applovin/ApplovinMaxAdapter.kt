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
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.auctions.AuctionRequest
import com.appodealstack.bidon.auctions.AuctionResult
import com.appodealstack.bidon.config.domain.AdapterInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.demands.*
import com.appodealstack.bidon.demands.banners.BannerSize
import com.appodealstack.bidon.demands.banners.BannerSizeKey
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter, Initializable<ApplovinParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner,
    PlacementSource by PlacementSourceImpl(),
    AdRevenueSource by AdRevenueSourceImpl(),
    ExtrasSource by ExtrasSourceImpl(),
    MediationNetwork {

    private lateinit var context: Context
    private val bannerAdUnitIds = mutableListOf<String>()
    private val interstitialAdUnitIds = mutableListOf<String>()
    private val rewardedAdUnitIds = mutableListOf<String>()

    override val mediationNetwork = BNMediationNetwork.ApplovinMax
    override val demandId: DemandId = ApplovinMaxDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = "3.2.1",
        bidonSdkVersion = "1.2.3"
    )

    override suspend fun init(activity: Activity, configParams: ApplovinParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            bannerAdUnitIds.addAll(configParams.bannerAdUnitIds)
            interstitialAdUnitIds.addAll(configParams.interstitialAdUnitIds)
            rewardedAdUnitIds.addAll(configParams.rewardedAdUnitIds)
            this.context = activity.applicationContext
            if (!AppLovinSdk.getInstance(context).isInitialized) {
                AppLovinSdk.getInstance(context).mediationProvider = "max"
                AppLovinSdk.initializeSdk(context) {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest {
        val adUnitId = adParams.getString(AdUnitIdKey) ?: bannerAdUnitIds.first()
        val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
            BannerSize.values()[it]
        }
        if (bannerSize !in arrayOf(BannerSize.Banner, BannerSize.LeaderBoard, BannerSize.MRec)) {
            return AuctionRequest {
                Result.failure(DemandError.BannerSizeNotSupported(demandId))
            }
        }
        logInternal("Tag", "1")
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
            /**
             * [AutoRefresher] provides auto-refresh
             */
            setExtraParameter("allow_pause_auto_refresh_immediately", "true")
            stopAutoRefresh()
        }
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxAdView.setListener(object : MaxAdViewAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = maxAd.asAd(demandAd)
                            val auctionResult = AuctionResult(
                                ad = ad,
                                adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider, PlacementProvider,
                                    AdViewProvider {
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

                                    override fun setPlacement(placement: String?) {
                                        maxAdView.placement = placement
                                    }

                                    override fun getPlacement(): String? = maxAdView.placement
                                }
                            )
                            maxAdView.setRevenueListener {
                                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
                                getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
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
                        maxAdView.destroy()
                    }

                    override fun onAdDisplayed(ad: MaxAd?) {}
                    override fun onAdHidden(ad: MaxAd?) {}
                    override fun onAdClicked(ad: MaxAd?) {}
                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
                    override fun onAdExpanded(ad: MaxAd?) {}
                    override fun onAdCollapsed(ad: MaxAd?) {}

                })
                maxAdView.loadAd()
            }
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity(demandId)) }
        val adUnitId = adParams.getString(AdUnitIdKey) ?: interstitialAdUnitIds.first()
        val maxInterstitialAd = MaxInterstitialAd(adUnitId, activity)
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxInterstitialAd.setListener(
                    object : MaxAdListener {
                        override fun onAdLoaded(maxAd: MaxAd) {
                            if (!isFinished.getAndSet(true)) {
                                val ad = maxAd.asAd(demandAd)
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
                                maxInterstitialAd.setRevenueListener {
                                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
                                    getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
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

                        override fun onAdDisplayed(ad: MaxAd?) {}
                        override fun onAdHidden(ad: MaxAd?) {}
                        override fun onAdClicked(ad: MaxAd?) {}
                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
                    }
                )
                maxInterstitialAd.loadAd()
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity(demandId)) }
        val adUnitId = adParams.getString(AdUnitIdKey) ?: rewardedAdUnitIds.first()
        val rewardedAd = MaxRewardedAd.getInstance(adUnitId, activity)
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                rewardedAd.setListener(object : MaxRewardedAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = maxAd.asAd(demandAd)
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

                            rewardedAd.setRevenueListener {
                                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
                                getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
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

                    override fun onAdDisplayed(ad: MaxAd?) {}
                    override fun onAdHidden(ad: MaxAd?) {}
                    override fun onAdClicked(ad: MaxAd?) {}
                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
                    override fun onRewardedVideoStarted(ad: MaxAd?) {}
                    override fun onRewardedVideoCompleted(ad: MaxAd?) {}
                    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {}
                })
                rewardedAd.loadAd()
            }
        }
    }

    private fun BannerSize.asMaxAdFormat() = when (this) {
        BannerSize.Banner -> MaxAdFormat.BANNER
        BannerSize.LeaderBoard -> MaxAdFormat.LEADER
        BannerSize.MRec -> MaxAdFormat.MREC
        else -> error("Not supported")
    }

}

internal fun MaxAd?.asAd(demandAd: DemandAd): Ad {
    val maxAd = this
    return Ad(
        demandId = ApplovinMaxDemandId,
        demandAd = demandAd,
        price = maxAd?.revenue ?: 0.0,
        sourceAd = maxAd ?: demandAd,
        monetizationNetwork = maxAd?.networkName,
        dsp = maxAd?.dspId,
        auctionRound = Ad.AuctionRound.Mediation,
        currencyCode = null
    )
}

internal const val AdUnitIdKey = "adUnitId"
internal const val PlacementKey = "placement"
internal const val CustomDataKey = "customData"
internal const val KeyKey = "key"
internal const val ValueKey = "valueKey"
internal const val AdaptiveBannerKey = "adaptive_banner"
internal const val AdaptiveBannerHeightKey = "AdaptiveBannerHeightKey"