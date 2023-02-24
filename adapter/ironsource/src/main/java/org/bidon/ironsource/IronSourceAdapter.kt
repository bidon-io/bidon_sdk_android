package org.bidon.ironsource

import android.app.Activity
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.InitializationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.ironsource.ext.adapterVersion
import org.bidon.ironsource.ext.sdkVersion
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.utils.SdkDispatchers
import org.json.JSONObject
import kotlin.coroutines.resume

val IronSourceDemandId = DemandId("ironsource")

class IronSourceAdapter :
    Adapter,
    Initializable<IronSourceParameters> {

    override val demandId: DemandId = IronSourceDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.Default)
    private val interstitialFlow = MutableSharedFlow<InterstitialInterceptor>(Int.MAX_VALUE)
    private val rewardedFlow = MutableSharedFlow<RewardedInterceptor>(Int.MAX_VALUE)
    private var interstitialDemandAd: DemandAd? = null
    private var rewardedDemandAd: DemandAd? = null

    override suspend fun init(activity: Activity, configParams: IronSourceParameters): Unit = suspendCancellableCoroutine {
        val initializationListener = InitializationListener {
            it.resume(Unit)
        }
        IronSource.init(activity, configParams.appKey, initializationListener)
    }

//    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: AdSource.AdParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            interstitialDemandAd = demandAd
//            IronSource.loadInterstitial()
//            val loadingResult = interstitialFlow.first {
//                it is InterstitialInterceptor.AdLoadFailed || it is InterstitialInterceptor.AdReady
//            }
//            when (loadingResult) {
//                is InterstitialInterceptor.AdReady -> {
//                    Result.success(
//                        OldAuctionResult(
//                            ad = loadingResult.adInfo.asAd(
//                                demandAd = demandAd,
//                            ),
//                            adProvider = object : OldAdProvider {
//                                override fun canShow() = IronSource.isInterstitialReady()
//                                override fun destroy() {}
//
//                                override fun showAd(activity: Activity?, adParams: Bundle) {
//                                    val placementId = adParams.getString(PlacementKey)
//                                    if (placementId.isNullOrBlank()) {
//                                        IronSource.showInterstitial()
//                                    } else {
//                                        IronSource.showInterstitial(placementId)
//                                    }
//                                }
//                            }
//                        )
//                    )
//                }
//                is InterstitialInterceptor.AdLoadFailed -> {
//                    Result.failure(loadingResult.ironSourceError.asBidonError())
//                }
//                else -> error("Unexpected state")
//            }
//        }
//    }
//
//    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: AdSource.AdParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            rewardedDemandAd = demandAd
//            IronSource.loadRewardedVideo()
//            val loadingResult = rewardedFlow.first {
//                it is RewardedInterceptor.AdLoadFailed || it is RewardedInterceptor.AdReady
//            }
//            when (loadingResult) {
//                is RewardedInterceptor.AdReady -> {
//                    Result.success(
//                        OldAuctionResult(
//                            ad = loadingResult.adInfo.asAd(demandAd),
//                            adProvider = object : OldAdProvider {
//                                override fun canShow() = IronSource.isRewardedVideoAvailable()
//                                override fun destroy() {}
//
//                                override fun showAd(activity: Activity?, adParams: Bundle) {
//                                    val placementId = adParams.getString(PlacementKey)
//                                    if (placementId.isNullOrBlank()) {
//                                        IronSource.showRewardedVideo()
//                                    } else {
//                                        IronSource.showRewardedVideo(placementId)
//                                    }
//                                }
//                            }
//                        )
//                    )
//                }
//                is RewardedInterceptor.AdLoadFailed -> {
//                    Result.failure(loadingResult.ironSourceError.asBidonError())
//                }
//                else -> error("Unexpected state")
//            }
//        }
//    }
//
//    override fun banner(context: Context, demandAd: DemandAd, adParams: ISBannerParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            val placementId = demandAd.placement
//            val isBannerSize = when (adParams.bannerSize) {
//                BannerSize.Banner -> ISBannerSize.BANNER
//                BannerSize.Large -> ISBannerSize.LARGE
//                BannerSize.MRec -> ISBannerSize.RECTANGLE
//                BannerSize.Smart -> ISBannerSize.SMART
//                BannerSize.LeaderBoard -> error("Not supported")
//            }
//            val bannerView = IronSource.createBanner(context as Activity, isBannerSize)
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                bannerView.levelPlayBannerListener = object : LevelPlayBannerListener {
//                    override fun onAdLoaded(adInfo: AdInfo?) {
//                        if (!isFinished.getAndSet(true)) {
//                            val ad = adInfo.asAd(demandAd)
//                            bannerView.setCoreListener(demandAd)
//                            continuation.resume(
//                                Result.success(
//                                    OldAuctionResult(
//                                        ad = ad,
//                                        adProvider = object : OldAdProvider, AdViewProvider {
//                                            override fun getAdView(): View = bannerView
//                                            override fun canShow(): Boolean = true
//                                            override fun showAd(activity: Activity?, adParams: Bundle) {}
//                                            override fun destroy() {
//                                                IronSource.destroyBanner(bannerView)
//                                            }
//
//                                        }
//                                    )
//                                )
//                            )
//                        }
//                    }
//
//                    override fun onAdLoadFailed(ironSourceError: IronSourceError?) {
//                        if (!isFinished.getAndSet(true)) {
//                            bannerView.removeBannerListener()
//                            continuation.resume(Result.failure(ironSourceError.asBidonError()))
//                        }
//                    }
//
//                    override fun onAdClicked(adInfo: AdInfo?) {}
//                    override fun onAdLeftApplication(adInfo: AdInfo?) {}
//                    override fun onAdScreenPresented(adInfo: AdInfo?) {}
//                    override fun onAdScreenDismissed(adInfo: AdInfo?) {}
//                }
//                if (placementId.isNullOrBlank()) {
//                    IronSource.loadBanner(bannerView)
//                } else {
//                    IronSource.loadBanner(bannerView, placementId)
//                }
//            }
//        }
//    }

    override fun parseConfigParam(json: String): IronSourceParameters {
        val jsonObject = JSONObject(json)
        return IronSourceParameters(
            appKey = jsonObject.getString("app_key"),
        )
    }

//    override fun interstitialParams(pricefloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for IronSource interstitial")
//    }
//
//    override fun rewardedParams(pricefloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        error("No additional params for IronSource rewarded")
//    }
//
//    override fun bannerParams(
//        pricefloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams = ISBannerParams(bannerSize)
//
//    private fun IronSourceBannerLayout.setCoreListener(demandAd: DemandAd) {
//        val bannerView = this
//        val coreListener = SdkCore.getListenerForDemand(demandAd)
//        bannerView.levelPlayBannerListener = object : LevelPlayBannerListener {
//            override fun onAdLoaded(adInfo: AdInfo?) {}
//            override fun onAdLoadFailed(p0: IronSourceError?) {}
//            override fun onAdLeftApplication(adInfo: AdInfo?) {}
//
//            override fun onAdClicked(adInfo: AdInfo?) {
//                coreListener.onAdClicked(adInfo.asAd(demandAd))
//            }
//
//            override fun onAdScreenPresented(adInfo: AdInfo?) {
//                val ad = adInfo.asAd(demandAd)
//                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(ad)
//                coreListener.onAdShown(ad)
//            }
//
//            override fun onAdScreenDismissed(adInfo: AdInfo?) {
//                coreListener.onAdClosed(adInfo.asAd(demandAd))
//            }
//        }
//    }

    private fun AdInfo?.asAd(demandAd: DemandAd): Ad {
        val adInfo = this
        return Ad(
            demandAd = demandAd,
            eCPM = adInfo?.revenue ?: 0.0,
            sourceAd = adInfo ?: demandAd,
            currencyCode = null,
            roundId = "Ad.AuctionRound.Mediation",
            dsp = null,
            networkName = adInfo?.adNetwork,
            auctionId = "auctionId",
            adUnitId = this?.adUnit
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
