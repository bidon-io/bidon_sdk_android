package com.appodealstack.ironsource

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.auctions.AuctionRequest
import com.appodealstack.bidon.auctions.AuctionResult
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.adapters.banners.BannerSizeKey
import com.appodealstack.ironsource.ext.adapterVersion
import com.appodealstack.ironsource.ext.sdkVersion
import com.appodealstack.ironsource.impl.asBidonError
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
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val IronSourceDemandId = DemandId("ironsource")

class IronSourceAdapter : Adapter, Initializable<IronSourceParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner, MediationNetwork {

    override val mediationNetwork = BNMediationNetwork.IronSource
    override val demandId: DemandId = IronSourceDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

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
            it.resume(Unit)
        }
        IronSource.init(activity, configParams.appKey, initializationListener)
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
                            ad = loadingResult.adInfo.asAd(
                                demandAd = demandAd,
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
                            ad = loadingResult.adInfo.asAd(demandAd),
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
                    coreListener.onAdClicked(callback.adInfo.asAd(demandAd))
                }
                is InterstitialInterceptor.AdClosed -> {
                    coreListener.onAdHidden(callback.adInfo.asAd(demandAd))
                }
                is InterstitialInterceptor.AdOpened -> {
                    coreListener.onAdImpression(callback.adInfo.asAd(demandAd))
                }
                is InterstitialInterceptor.AdShowFailed -> {
                    coreListener.onAdDisplayFailed(callback.ironSourceError.asBidonError())
                }
                is InterstitialInterceptor.AdShowSucceeded -> {
                    val ad = callback.adInfo.asAd(demandAd)
                    coreListener.onAdDisplayed(callback.adInfo.asAd(demandAd))
                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
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
                    coreListener.onAdClicked(callback.adInfo.asAd(demandAd))
                }
                is RewardedInterceptor.AdClosed -> {
                    coreListener.onAdHidden(callback.adInfo.asAd(demandAd))
                }
                is RewardedInterceptor.AdOpened -> {
                    coreListener.onAdImpression(callback.adInfo.asAd(demandAd))
                }
                is RewardedInterceptor.AdShowFailed -> {
                    coreListener.onAdDisplayFailed(callback.ironSourceError.asBidonError())
                }
                is RewardedInterceptor.Rewarded -> {
                    val ad = callback.adInfo.asAd(demandAd)
                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
                    coreListener.onUserRewarded(
                        ad = ad,
                        reward = RewardedAdListener.Reward(
                            label = callback.placement?.rewardName ?: "",
                            amount = callback.placement?.rewardAmount ?: 0
                        )
                    )
                }
                RewardedInterceptor.Started -> {
                    coreListener.onRewardedStarted(null.asAd(demandAd))
                }
                RewardedInterceptor.Ended -> {
                    coreListener.onRewardedCompleted(null.asAd(demandAd))
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
                            val ad = adInfo.asAd(demandAd)
                            bannerView.setCoreListener(demandAd)
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

    override fun parseConfigParam(json: JsonObject): IronSourceParameters = json.parse(IronSourceParameters.serializer())

    private fun IronSourceBannerLayout.setCoreListener(demandAd: DemandAd) {
        val bannerView = this
        val coreListener = SdkCore.getListenerForDemand(demandAd)
        bannerView.levelPlayBannerListener = object : LevelPlayBannerListener {
            override fun onAdLoaded(adInfo: AdInfo?) {}
            override fun onAdLoadFailed(p0: IronSourceError?) {}
            override fun onAdLeftApplication(adInfo: AdInfo?) {}

            override fun onAdClicked(adInfo: AdInfo?) {
                coreListener.onAdClicked(adInfo.asAd(demandAd))
            }

            override fun onAdScreenPresented(adInfo: AdInfo?) {
                val ad = adInfo.asAd(demandAd)
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
                coreListener.onAdDisplayed(ad)
            }

            override fun onAdScreenDismissed(adInfo: AdInfo?) {
                coreListener.onAdHidden(adInfo.asAd(demandAd))
            }
        }
    }

    private fun AdInfo?.asAd(demandAd: DemandAd): Ad {
        val adInfo = this
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = adInfo?.revenue ?: 0.0,
            sourceAd = adInfo ?: demandAd,
            currencyCode = null,
            auctionRound = Ad.AuctionRound.Mediation,
            dsp = null,
            monetizationNetwork = adInfo?.adNetwork
        )
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