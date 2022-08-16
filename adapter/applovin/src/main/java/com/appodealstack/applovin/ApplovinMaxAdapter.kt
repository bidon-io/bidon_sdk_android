package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.adapterVersion
import com.appodealstack.applovin.ext.sdkVersion
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter, Initializable<ApplovinParameters>,
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

    override fun parseConfigParam(json: JsonObject): ApplovinParameters = json.parse(ApplovinParameters.serializer())

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
//    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: ApplovinFullscreenAdParams): OldAuctionRequest {
//        if (activity == null) return OldAuctionRequest { Result.failure(DemandError.NoActivity(demandId)) }
//        val maxInterstitialAd = MaxInterstitialAd(adParams.adUnitId, activity)
//        return OldAuctionRequest {
//            suspendCancellableCoroutine { continuation ->
//                val isFinished = AtomicBoolean(false)
//                maxInterstitialAd.setListener(
//                    object : MaxAdListener {
//                        override fun onAdLoaded(maxAd: MaxAd) {
//                            if (!isFinished.getAndSet(true)) {
//                                val ad = maxAd.asAd(demandAd)
//                                val auctionResult = OldAuctionResult(
//                                    ad = ad,
//                                    adProvider = object : OldAdProvider, AdRevenueProvider, ExtrasProvider {
//                                        override fun canShow(): Boolean {
//                                            return maxInterstitialAd.isReady
//                                        }
//
//                                        override fun showAd(activity: Activity?, adParams: Bundle) {
//                                            val placement = adParams.getString(PlacementKey)
//                                            val customData = adParams.getString(CustomDataKey)
//                                            maxInterstitialAd.showAd(placement, customData)
//                                        }
//
//                                        override fun destroy() = maxInterstitialAd.destroy()
//
//                                        override fun setAdRevenueListener(adRevenueListener: AdRevenueListener) {
//                                            maxInterstitialAd.setRevenueListener {
//                                                adRevenueListener.onAdRevenuePaid(ad)
//                                            }
//                                        }
//
//                                        override fun setExtras(adParams: Bundle) {
//                                            adParams.keySet().forEach { key ->
//                                                if (adParams.get(key) is String) {
//                                                    maxInterstitialAd.setExtraParameter(key, adParams.getString(key))
//                                                }
//                                            }
//                                        }
//
//                                    }
//                                )
//                                maxInterstitialAd.setRevenueListener {
//                                    SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(it.asAd(demandAd))
//                                    getUserAdRevenueListener(demandAd)?.onAdRevenuePaid(ad)
//                                }
//                                getExtras(demandAd)?.let { bundle ->
//                                    bundle.keySet().forEach { key ->
//                                        if (bundle.get(key) is String) {
//                                            maxInterstitialAd.setExtraParameter(key, bundle.getString(key))
//                                        }
//                                    }
//                                }
//                                maxInterstitialAd.setCoreListener(auctionResult)
//                                continuation.resume(Result.success(auctionResult))
//                            }
//                        }
//
//                        override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
//                            if (!isFinished.getAndSet(true)) {
//                                // remove listener
//                                maxInterstitialAd.setListener(null)
//                                continuation.resume(Result.failure(error.asBidonError()))
//                            }
//                        }
//
//                        override fun onAdDisplayed(ad: MaxAd?) {}
//                        override fun onAdHidden(ad: MaxAd?) {}
//                        override fun onAdClicked(ad: MaxAd?) {}
//                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
//                    }
//                )
//                maxInterstitialAd.loadAd()
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
//    override fun interstitialParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        TODO("Not yet implemented")
//    }
//
//    override fun rewardedParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        TODO("Not yet implemented")
//    }
//
//    override fun bannerParams(
//        priceFloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams {
//        TODO("Not yet implemented")
//    }

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
        roundId = "Ad.AuctionRound.Mediation",
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