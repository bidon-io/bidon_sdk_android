package com.appodealstack.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.ironsource.impl.asBidonError
import com.appodealstack.ironsource.interstitial.addInterstitialListener
import com.appodealstack.ironsource.rewarded.addRewardedListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.banners.BannerSize
import com.appodealstack.mads.demands.banners.BannerSizeKey
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.InitializationListener
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

data class IronSourceParameters(val appKey: String, val adUnit: IronSource.AD_UNIT? = null) : AdapterParameters

val IronSourceDemandId = DemandId("ironsource")

class IronSourceAdapter : Adapter.Mediation<IronSourceParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner {
    override val demandId: DemandId = IronSourceDemandId

    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)
    private val interstitialFlow = MutableSharedFlow<InterstitialInterceptor>(Int.MAX_VALUE)
    private val rewardedFlow = MutableSharedFlow<RewardedInterceptor>(Int.MAX_VALUE)
    private var interstitialDemandAd: DemandAd? = null
    private var rewardedDemandAd: DemandAd? = null

    init {
        scope.launch {
            interstitialFlow.collect { callback ->
                onInterstitialCallbackIntercepted(callback)
            }
        }
        scope.launch {
            rewardedFlow.collect { callback ->
                onRewardedCallbackIntercepted(callback)
            }
        }
    }

    override suspend fun init(activity: Activity, configParams: IronSourceParameters): Unit = suspendCancellableCoroutine {
        val initializationListener = InitializationListener {
            interstitialFlow.addInterstitialListener()
            rewardedFlow.addRewardedListener()
            it.resume(Unit)
        }
        if (configParams.adUnit == null) {
            IronSource.init(activity, configParams.appKey, initializationListener)
        } else {
            IronSource.init(activity, configParams.appKey, initializationListener, configParams.adUnit)
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            interstitialDemandAd = demandAd
            IronSource.loadInterstitial()
            val loadingResult = interstitialFlow.first {
                it is InterstitialInterceptor.AdLoadFailed || it is InterstitialInterceptor.AdReady
            }
            when (loadingResult) {
                is InterstitialInterceptor.AdReady -> {
                    Result.success(
                        AuctionResult(
                            ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = loadingResult.adInfo?.revenue ?: 0.0,
                                sourceAd = loadingResult.adInfo ?: demandAd
                            ),
                            adProvider = object : AdProvider {
                                override fun canShow() = IronSource.isInterstitialReady()
                                override fun destroy() {}

                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                    val placementId = adParams.getString(PlacementKey)
                                    if (placementId.isNullOrBlank()) {
                                        IronSource.showInterstitial()
                                    } else {
                                        IronSource.showInterstitial(placementId)
                                    }
                                }
                            }
                        )
                    )
                }
                is InterstitialInterceptor.AdLoadFailed -> {
                    Result.failure(loadingResult.ironSourceError.asBidonError())
                }
                else -> error("Unexpected state")
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            rewardedDemandAd = demandAd
            IronSource.loadRewardedVideo()
            val loadingResult = rewardedFlow.first {
                it is RewardedInterceptor.AdLoadFailed || it is RewardedInterceptor.AdReady
            }
            when (loadingResult) {
                is RewardedInterceptor.AdReady -> {
                    Result.success(
                        AuctionResult(
                            ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = loadingResult.adInfo?.revenue ?: 0.0,
                                sourceAd = loadingResult.adInfo ?: demandAd
                            ),
                            adProvider = object : AdProvider {
                                override fun canShow() = IronSource.isRewardedVideoAvailable()
                                override fun destroy() {}

                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                    val placementId = adParams.getString(PlacementKey)
                                    if (placementId.isNullOrBlank()) {
                                        IronSource.showRewardedVideo()
                                    } else {
                                        IronSource.showRewardedVideo(placementId)
                                    }
                                }
                            }
                        )
                    )
                }
                is RewardedInterceptor.AdLoadFailed -> {
                    Result.failure(loadingResult.ironSourceError.asBidonError())
                }
                else -> error("Unexpected state")
            }
        }
    }

    private fun onInterstitialCallbackIntercepted(callback: InterstitialInterceptor) {
        interstitialDemandAd?.let { demandAd ->
            val coreListener = SdkCore.getListenerForDemand(demandAd)
            when (callback) {
                is InterstitialInterceptor.AdClicked -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdClicked(ad)
                }
                is InterstitialInterceptor.AdClosed -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdHidden(ad)
                }
                is InterstitialInterceptor.AdOpened -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdImpression(ad)
                }
                is InterstitialInterceptor.AdShowFailed -> {
                    coreListener.onAdDisplayFailed(callback.ironSourceError.asBidonError())
                }
                is InterstitialInterceptor.AdShowSucceeded -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdDisplayed(ad)
                }
                is InterstitialInterceptor.AdReady,
                is InterstitialInterceptor.AdLoadFailed -> {
                    // do nothing
                }
            }
        }
    }

    private fun onRewardedCallbackIntercepted(callback: RewardedInterceptor) {
        rewardedDemandAd?.let { demandAd ->
            val coreListener = SdkCore.getListenerForDemand(demandAd)
            when (callback) {
                is RewardedInterceptor.AdClicked -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdClicked(ad)
                }
                is RewardedInterceptor.AdClosed -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdHidden(ad)
                }
                is RewardedInterceptor.AdOpened -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onAdImpression(ad)
                }
                is RewardedInterceptor.AdShowFailed -> {
                    coreListener.onAdDisplayFailed(callback.ironSourceError.asBidonError())
                }
                is RewardedInterceptor.Rewarded -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = callback.adInfo?.revenue ?: 0.0,
                        sourceAd = callback.adInfo ?: demandAd
                    )
                    coreListener.onUserRewarded(
                        ad, RewardedAdListener.Reward(
                            label = callback.placement?.rewardName ?: "",
                            amount = callback.placement?.rewardAmount ?: 0
                        )
                    )
                }
                RewardedInterceptor.Started -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = 0.0,
                        sourceAd = demandAd
                    )
                    coreListener.onRewardedStarted(ad)
                }
                RewardedInterceptor.Ended -> {
                    val ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = 0.0,
                        sourceAd = demandAd
                    )
                    coreListener.onRewardedCompleted(ad)
                }
                is RewardedInterceptor.AdReady,
                is RewardedInterceptor.AdLoadFailed -> {
                    // do nothing
                }
            }
        }
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest {
        return AuctionRequest {
            val placementId = adParams.getString(PlacementKey)
            val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
                BannerSize.values()[it]
            }
            val isBannerSize = when (bannerSize) {
                BannerSize.Banner -> ISBannerSize.BANNER
                BannerSize.Large -> ISBannerSize.LARGE
                BannerSize.MRec -> ISBannerSize.RECTANGLE
                BannerSize.Smart -> ISBannerSize.SMART
                BannerSize.LeaderBoard -> error("Not supported")
            }
            val bannerView = IronSource.createBanner(context as Activity, isBannerSize)
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                bannerView.levelPlayBannerListener = object : LevelPlayBannerListener {
                    override fun onAdLoaded(adInfo: AdInfo?) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = Ad(
                                demandId = demandId,
                                demandAd = demandAd,
                                price = adInfo?.revenue ?: 0.0,
                                sourceAd = adInfo ?: bannerView
                            )
                            bannerView.setCoreListener(ad)
                            continuation.resume(
                                Result.success(
                                    AuctionResult(
                                        ad = ad,
                                        adProvider = object : AdProvider, AdViewProvider {
                                            override fun getAdView(): View = bannerView
                                            override fun canShow(): Boolean = true
                                            override fun showAd(activity: Activity?, adParams: Bundle) {}
                                            override fun destroy() {
                                                IronSource.destroyBanner(bannerView)
                                            }

                                        }
                                    )
                                )
                            )
                        }
                    }

                    override fun onAdLoadFailed(ironSourceError: IronSourceError?) {
                        if (!isFinished.getAndSet(true)) {
                            bannerView.removeBannerListener()
                            continuation.resume(Result.failure(ironSourceError.asBidonError()))
                        }
                    }

                    override fun onAdClicked(adInfo: AdInfo?) {}
                    override fun onAdLeftApplication(adInfo: AdInfo?) {}
                    override fun onAdScreenPresented(adInfo: AdInfo?) {}
                    override fun onAdScreenDismissed(adInfo: AdInfo?) {}
                }
                if (placementId.isNullOrBlank()) {
                    IronSource.loadBanner(bannerView)
                } else {
                    IronSource.loadBanner(bannerView, placementId)
                }
            }
        }
    }

    private fun IronSourceBannerLayout.setCoreListener(ad: Ad) {
        val bannerView = this
        val coreListener = SdkCore.getListenerForDemand(ad.demandAd)
        bannerView.levelPlayBannerListener = object : LevelPlayBannerListener {
            override fun onAdLoaded(p0: AdInfo?) {}
            override fun onAdLoadFailed(p0: IronSourceError?) {}
            override fun onAdLeftApplication(p0: AdInfo?) {}

            override fun onAdClicked(p0: AdInfo?) {
                coreListener.onAdClicked(ad)
            }

            override fun onAdScreenPresented(p0: AdInfo?) {
                coreListener.onAdDisplayed(ad)
            }

            override fun onAdScreenDismissed(p0: AdInfo?) {
                coreListener.onAdHidden(ad)
            }
        }
    }
}

internal sealed interface InterstitialInterceptor {
    class AdReady(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdLoadFailed(val ironSourceError: IronSourceError?) : InterstitialInterceptor
    class AdOpened(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdShowSucceeded(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdShowFailed(val adInfo: AdInfo?, val ironSourceError: IronSourceError?) : InterstitialInterceptor
    class AdClicked(val adInfo: AdInfo?) : InterstitialInterceptor
    class AdClosed(val adInfo: AdInfo?) : InterstitialInterceptor
}

internal sealed interface RewardedInterceptor {
    class AdReady(val adInfo: AdInfo?) : RewardedInterceptor
    class AdLoadFailed(val ironSourceError: IronSourceError?) : RewardedInterceptor
    class AdOpened(val adInfo: AdInfo?) : RewardedInterceptor
    class AdClosed(val adInfo: AdInfo?) : RewardedInterceptor
    class AdShowFailed(val adInfo: AdInfo?, val ironSourceError: IronSourceError?) : RewardedInterceptor
    class AdClicked(val placement: Placement?, val adInfo: AdInfo?) : RewardedInterceptor
    class Rewarded(val placement: Placement?, val adInfo: AdInfo?) : RewardedInterceptor
    object Started : RewardedInterceptor
    object Ended : RewardedInterceptor
}


const val PlacementKey = "placement"