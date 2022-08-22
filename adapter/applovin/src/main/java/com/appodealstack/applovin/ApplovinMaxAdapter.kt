package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.adapterVersion
import com.appodealstack.applovin.ext.sdkVersion
import com.appodealstack.applovin.impl.MaxInterstitialImpl
import com.appodealstack.applovin.impl.MaxRewardedImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter, Initializable<ApplovinParameters>,
    AdProvider.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdProvider.Rewarded<ApplovinFullscreenAdAuctionParams>,
    AdRevenueSource by AdRevenueSourceImpl(),
    ExtrasSource by ExtrasSourceImpl(),
    MediationNetwork {

    private lateinit var context: Context

    override val mediationNetwork = BNMediationNetwork.ApplovinMax
    override val demandId: DemandId = ApplovinMaxDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: ApplovinParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            val context = activity.applicationContext.also {
                context = it
            }
//            val instance = AppLovinSdk.getInstance(configParams.key, AppLovinSdkSettings(context), context)
            val instance = AppLovinSdk.getInstance(context)
            instance.settings.setVerboseLogging(true)
            if (!instance.isInitialized) {
                instance.mediationProvider = "max"
                instance.initializeSdk {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: JsonObject): ApplovinParameters = json.parse(ApplovinParameters.serializer())

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return MaxInterstitialImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {
        return MaxRewardedImpl(demandId, demandAd, roundId)
    }

    //    override fun banner(context: Context, demandAd: DemandAd, adParams: ApplovinBannerParams): OldAuctionRequest {
//        if (adParams.bannerSize !in arrayOf(BannerSize.Banner, BannerSize.LeaderBoard, BannerSize.MRec)) {
//            return OldAuctionRequest {
//                Result.failure(DemandError.BannerSizeNotSupported(demandId))
//            }
//        }
//        logInternal("Tag", "1")
//        val maxAdView = MaxAdView(adParams.adUnitId, adParams.bannerSize.asMaxAdFormat(), context).apply {
//            val isAdaptive = getExtras(demandAd)?.getString(AdaptiveBannerKey, "-")
//            if (isAdaptive == "true") {
//                val width = ViewGroup.LayoutParams.MATCH_PARENT
//                // Height calculating should be done on application side and passed with extras[AdaptiveBannerHeightKey]
//                val heightPx = adParams.adaptiveBannerHeight ?: ViewGroup.LayoutParams.WRAP_CONTENT
//                layoutParams = FrameLayout.LayoutParams(width, heightPx)
//            }
//            getExtras(demandAd)?.let { bundle ->
//                bundle.keySet().forEach { key ->
//                    if (bundle.get(key) is String) {
//                        setExtraParameter(key, bundle.getString(key))
//                    }
//                }
//            }
//            demandAd.placement?.let {
//                placement = it
//            }
//            /**
//             * [AutoRefresher] provides auto-refresh
//             */
//            setExtraParameter("allow_pause_auto_refresh_immediately", "true")
//            stopAutoRefresh()
//        }
//        return OldAuctionRequest {
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                maxAdView.setListener(object : MaxAdViewAdListener {
//                    override fun onAdLoaded(maxAd: MaxAd) {
//                        if (!isFinished.getAndSet(true)) {
//                            val ad = maxAd.asAd(demandAd)
//                            val auctionResult = OldAuctionResult(
//                                ad = ad,
//                                adProvider = object : OldAdProvider, AdRevenueProvider, ExtrasProvider, PlacementProvider,
//                                    AdViewProvider {
//                                    override fun canShow(): Boolean = true
//                                    override fun showAd(activity: Activity?, adParams: Bundle) {}
//                                    override fun destroy() = maxAdView.destroy()
//                                    override fun getAdView(): View = maxAdView
//
//                                    override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
//                                        maxAdView.setRevenueListener {
//                                            adRevenueListener.onAdRevenuePaid(ad)
//                                        }
//                                    }
//
//                                    override fun setExtras(adParams: Bundle) {
//                                        adParams.keySet().forEach { key ->
//                                            if (adParams.get(key) is String) {
//                                                maxAdView.setExtraParameter(key, adParams.getString(key))
//                                            }
//                                        }
//                                    }
//
//                                    override fun setPlacement(placement: String?) {
//                                        maxAdView.placement = placement
//                                    }
//
//                                    override fun getPlacement(): String? = maxAdView.placement
//                                }
//                            )
//                            maxAdView.setRevenueListener {
//                                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
//                                getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
//                            }
//                            maxAdView.setCoreListener(auctionResult)
//                            continuation.resume(Result.success(auctionResult))
//                        }
//                    }
//
//                    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//                        if (!isFinished.getAndSet(true)) {
//                            // remove listener
//                            maxAdView.setListener(null)
//                            continuation.resume(Result.failure(error.asBidonError()))
//                        }
//                        maxAdView.destroy()
//                    }
//
//                    override fun onAdDisplayed(ad: MaxAd?) {}
//                    override fun onAdHidden(ad: MaxAd?) {}
//                    override fun onAdClicked(ad: MaxAd?) {}
//                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
//                    override fun onAdExpanded(ad: MaxAd?) {}
//                    override fun onAdCollapsed(ad: MaxAd?) {}
//
//                })
//                maxAdView.loadAd()
//            }
//        }
//    }
//
//    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: ApplovinFullscreenAdParams): OldAuctionRequest {
//        if (activity == null) return OldAuctionRequest { Result.failure(DemandError.NoActivity(demandId)) }
//        val rewardedAd = MaxRewardedAd.getInstance(adParams.adUnitId, activity)
//        return OldAuctionRequest {
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                rewardedAd.setListener(object : MaxRewardedAdListener {
//                    override fun onAdLoaded(maxAd: MaxAd) {
//                        if (!isFinished.getAndSet(true)) {
//                            val ad = maxAd.asAd(demandAd)
//                            val auctionResult = OldAuctionResult(
//                                ad = ad,
//                                adProvider = object : OldAdProvider, AdRevenueProvider, ExtrasProvider {
//                                    override fun canShow(): Boolean = rewardedAd.isReady
//
//                                    override fun showAd(activity: Activity?, adParams: Bundle) {
//                                        val placement = adParams.getString(PlacementKey)
//                                        val customData = adParams.getString(CustomDataKey)
//                                        rewardedAd.showAd(placement, customData)
//                                    }
//
//                                    override fun destroy() = rewardedAd.destroy()
//
//                                    override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
//                                        rewardedAd.setRevenueListener {
//                                            adRevenueListener.onAdRevenuePaid(ad)
//                                        }
//                                    }
//
//                                    override fun setExtras(adParams: Bundle) {
//                                        adParams.keySet().forEach { key ->
//                                            if (adParams.get(key) is String) {
//                                                rewardedAd.setExtraParameter(key, adParams.getString(key))
//                                            }
//                                        }
//                                    }
//
//                                }
//                            )
//
//                            rewardedAd.setRevenueListener {
//                                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
//                                getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
//                            }
//                            getExtras(demandAd)?.let { bundle ->
//                                bundle.keySet().forEach { key ->
//                                    if (bundle.get(key) is String) {
//                                        rewardedAd.setExtraParameter(key, bundle.getString(key))
//                                    }
//                                }
//                            }
//                            rewardedAd.setCoreListener(auctionResult)
//                            continuation.resume(Result.success(auctionResult))
//                        }
//
//                    }
//
//                    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//                        if (!isFinished.getAndSet(true)) {
//                            // remove listener
//                            rewardedAd.setListener(null)
//                            continuation.resume(Result.failure(error.asBidonError()))
//                        }
//                    }
//
//                    override fun onAdDisplayed(ad: MaxAd?) {}
//                    override fun onAdHidden(ad: MaxAd?) {}
//                    override fun onAdClicked(ad: MaxAd?) {}
//                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
//                    override fun onRewardedVideoStarted(ad: MaxAd?) {}
//                    override fun onRewardedVideoCompleted(ad: MaxAd?) {}
//                    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {}
//                })
//                rewardedAd.loadAd()
//            }
//        }
//    }
//

}

internal const val AdUnitIdKey = "adUnitId"
internal const val PlacementKey = "placement"
internal const val CustomDataKey = "customData"
internal const val KeyKey = "key"
internal const val ValueKey = "valueKey"
internal const val AdaptiveBannerKey = "adaptive_banner"
internal const val AdaptiveBannerHeightKey = "AdaptiveBannerHeightKey"