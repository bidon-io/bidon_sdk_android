package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import com.appodealstack.admob.ext.adapterVersion
import com.appodealstack.admob.ext.sdkVersion
import com.appodealstack.admob.impl.AdmobInterstitialImpl
import com.appodealstack.admob.impl.AdmobRewardedImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.google.android.gms.ads.*
import kotlinx.serialization.json.JsonObject
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@JvmInline
private value class AdUnitId(val value: String)

class AdmobAdapter : Adapter, Initializable<AdmobInitParameters>,
    AdProvider.Rewarded<AdmobFullscreenAdAuctionParams>,
    AdProvider.Interstitial<AdmobFullscreenAdAuctionParams> {
    private lateinit var context: Context

    override val demandId = AdmobDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: AdmobInitParameters): Unit = suspendCoroutine { continuation ->
        this.context = activity.applicationContext
        /**
         * Don't forget set Automatic refresh is Disabled for each AdUnit.
         * Manage refresh rate with [AutoRefresher.setAutoRefresh].
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<AdmobFullscreenAdAuctionParams> {
        return AdmobInterstitialImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<AdmobFullscreenAdAuctionParams> {
        return AdmobRewardedImpl(demandId, demandAd, roundId)
    }

    //    override fun banner(context: Context, demandAd: DemandAd, adParams: AdmobBannerParams): OldAuctionRequest {
//        return OldAuctionRequest {
//            withContext(Dispatchers.Main) {
//                suspendCancellableCoroutine { continuation ->
//                    val isFinished = AtomicBoolean(false)
//                    val adRequest = AdRequest.Builder().build()
//                    val adView = AdView(context)
//                    val admobBannerSize = adParams.bannerSize.asAdmobAdSize()
//                    val adUnitId = adParams.admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
//                    when {
//                        adUnitId.isNullOrBlank() -> {
//                            continuation.resume(Result.failure(DemandError.NoAppropriateAdUnitId(demandId)))
//                        }
//                        admobBannerSize != null -> {
//                            adView.setAdSize(admobBannerSize)
//                            adView.adUnitId = adUnitId
//
//                            adView.adListener = object : AdListener() {
//                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                                    if (!isFinished.getAndSet(true)) {
//                                        continuation.resume(Result.failure(loadAdError.asBidonError()))
//                                    }
//                                }
//
//                                override fun onAdLoaded() {
//                                    val ad = asAd(
//                                        demandAd = demandAd,
//                                        price = adParams.admobLineItems.getPrice(unitId = adView.adUnitId),
//                                        sourceAd = adView
//                                    )
//                                    val auctionResult = OldAuctionResult(
//                                        ad = ad,
//                                        adProvider = object : OldAdProvider, AdViewProvider {
//                                            override fun canShow(): Boolean = true
//                                            override fun destroy() {
//                                                adView.destroy()
//                                            }
//
//                                            override fun showAd(activity: Activity?, adParams: Bundle) {}
//                                            override fun getAdView(): View = adView
//                                        }
//                                    )
//                                    adView.setCoreListener(demandAd, auctionResult)
//                                    continuation.resume(Result.success(auctionResult))
//                                }
//                            }
//                            adView.loadAd(adRequest)
//                        }
//                        else -> {
//                            continuation.resume(Result.failure(DemandError.BannerSizeNotSupported(demandId)))
//                        }
//                    }
//                }
//            }
//        }
//    }

    override fun parseConfigParam(json: JsonObject): AdmobInitParameters = AdmobInitParameters

//    override fun bannerParams(
//        priceFloor: Double,
//        lineItems: List<LineItem>,
//        bannerSize: BannerSize,
//        adContainer: ViewGroup?
//    ): AdSource.AdParams {
//        return AdmobBannerParams(
//            admobLineItems = lineItems.filterByDemandId(),
//            bannerSize = bannerSize,
//            adContainer = adContainer,
//            priceFloor = priceFloor
//        )
//    }
//
//    override fun interstitialParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        return AdmobFullscreenAdParams(
//            admobLineItems = lineItems.filterByDemandId(),
//            priceFloor = priceFloor
//        )
//    }
//
//    override fun rewardedParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
//        return AdmobFullscreenAdParams(
//            admobLineItems = lineItems.filterByDemandId(),
//            priceFloor = priceFloor
//        )
//    }

    private fun BannerSize.asAdmobAdSize() = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
        else -> null
    }
}