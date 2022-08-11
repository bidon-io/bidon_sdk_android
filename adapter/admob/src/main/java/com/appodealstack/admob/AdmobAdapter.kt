package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.admob.ext.adapterVersion
import com.appodealstack.admob.ext.sdkVersion
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.auctions.domain.AuctionRequest
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@JvmInline
private value class AdUnitId(val value: String)

class AdmobAdapter : Adapter, Initializable<AdmobParameters>,
    AdSource.Interstitial<AdmobFullscreenAdParams>, AdSource.Rewarded<AdmobFullscreenAdParams>,
    AdSource.Banner<AdmobBannerParams> {
    private lateinit var context: Context

    override val demandId = AdmobDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: AdmobParameters): Unit = suspendCoroutine { continuation ->
        this.context = activity.applicationContext
        /**
         * Don't forget set Automatic refresh is Disabled for each AdUnit.
         * Manage refresh rate with [AutoRefresher.setAutoRefresh].
         */
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: AdmobFullscreenAdParams): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val adUnitId = adParams.admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
                    if (adUnitId.isNullOrBlank()) {
                        continuation.resume(Result.failure(DemandError.NoAppropriateAdUnitId(demandId)))
                    } else {
                        val isFinished = AtomicBoolean(false)
                        val adRequest = AdRequest.Builder().build()
                        InterstitialAd.load(
                            context,
                            adUnitId,
                            adRequest,
                            object : InterstitialAdLoadCallback() {
                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    if (!isFinished.getAndSet(true)) {
                                        continuation.resume(Result.failure(loadAdError.asBidonError()))
                                    }
                                }

                                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                    if (!isFinished.getAndSet(true)) {
                                        val auctionResult = AuctionResult(
                                            ad = asAd(
                                                demandAd = demandAd,
                                                price = adParams.admobLineItems.getPrice(unitId = interstitialAd.adUnitId),
                                                sourceAd = interstitialAd
                                            ),
                                            adProvider = object : AdProvider {
                                                override fun canShow(): Boolean = true
                                                override fun destroy() {}

                                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                                    if (activity == null) {
                                                        logInternal(
                                                            "AdmobDemand",
                                                            "Error while showing InterstitialAd: activity is null."
                                                        )
                                                    } else {
                                                        interstitialAd.show(activity)
                                                    }
                                                }
                                            }
                                        )
                                        interstitialAd.setCoreListener(demandAd, auctionResult)
                                        continuation.resume(Result.success(auctionResult))
                                    }
                                }
                            })
                    }
                }
            }
        }
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: AdmobFullscreenAdParams): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val adUnitId = adParams.admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
                    if (adUnitId.isNullOrBlank()) {
                        continuation.resume(Result.failure(DemandError.NoAppropriateAdUnitId(demandId)))
                    } else {
                        val isFinished = AtomicBoolean(false)
                        val adRequest = AdRequest.Builder().build()
                        RewardedAd.load(
                            context,
                            adUnitId,
                            adRequest,
                            object : RewardedAdLoadCallback() {
                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    if (!isFinished.getAndSet(true)) {
                                        continuation.resume(Result.failure(loadAdError.asBidonError()))
                                    }
                                }

                                override fun onAdLoaded(rewardedAd: RewardedAd) {
                                    if (!isFinished.getAndSet(true)) {
                                        val ad = asAd(
                                            demandAd = demandAd,
                                            price = adParams.admobLineItems.getPrice(unitId = rewardedAd.adUnitId),
                                            sourceAd = rewardedAd
                                        )
                                        val auctionResult = AuctionResult(
                                            ad = ad,
                                            adProvider = object : AdProvider {
                                                override fun canShow(): Boolean = true
                                                override fun destroy() {}

                                                override fun showAd(activity: Activity?, adParams: Bundle) {
                                                    if (activity == null) {
                                                        logInternal(
                                                            "AdmobDemand",
                                                            "Error while showing RewardedAd: activity is null."
                                                        )
                                                    } else {
                                                        rewardedAd.show(activity) { rewardItem ->
                                                            logInternal("rew", "rewardedAd.show(activity) $rewardItem")
                                                            SdkCore.getListenerForDemand(demandAd).onUserRewarded(
                                                                ad = ad,
                                                                reward = RewardedAdListener.Reward(
                                                                    label = rewardItem.type,
                                                                    amount = rewardItem.amount
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                        rewardedAd.setCoreListener(demandAd, auctionResult)
                                        continuation.resume(Result.success(auctionResult))
                                    }
                                }
                            })
                    }
                }
            }
        }
    }

    override fun banner(context: Context, demandAd: DemandAd, adParams: AdmobBannerParams): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    val adView = AdView(context)
                    val admobBannerSize = adParams.bannerSize.asAdmobAdSize()
                    val adUnitId = adParams.admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
                    when {
                        adUnitId.isNullOrBlank() -> {
                            continuation.resume(Result.failure(DemandError.NoAppropriateAdUnitId(demandId)))
                        }
                        admobBannerSize != null -> {
                            adView.setAdSize(admobBannerSize)
                            adView.adUnitId = adUnitId

                            adView.adListener = object : AdListener() {
                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    if (!isFinished.getAndSet(true)) {
                                        continuation.resume(Result.failure(loadAdError.asBidonError()))
                                    }
                                }

                                override fun onAdLoaded() {
                                    val ad = asAd(
                                        demandAd = demandAd,
                                        price = adParams.admobLineItems.getPrice(unitId = adView.adUnitId),
                                        sourceAd = adView
                                    )
                                    val auctionResult = AuctionResult(
                                        ad = ad,
                                        adProvider = object : AdProvider, AdViewProvider {
                                            override fun canShow(): Boolean = true
                                            override fun destroy() {
                                                adView.destroy()
                                            }

                                            override fun showAd(activity: Activity?, adParams: Bundle) {}
                                            override fun getAdView(): View = adView
                                        }
                                    )
                                    adView.setCoreListener(demandAd, auctionResult)
                                    continuation.resume(Result.success(auctionResult))
                                }
                            }
                            adView.loadAd(adRequest)
                        }
                        else -> {
                            continuation.resume(Result.failure(DemandError.BannerSizeNotSupported(demandId)))
                        }
                    }
                }
            }
        }
    }

    override fun parseConfigParam(json: JsonObject): AdmobParameters = AdmobParameters

    override fun bannerParams(
        priceFloor: Double,
        lineItems: List<LineItem>,
        bannerSize: BannerSize,
        adContainer: ViewGroup?
    ): AdSource.AdParams {
        return AdmobBannerParams(
            admobLineItems = lineItems.filterByDemandId(),
            bannerSize = bannerSize,
            adContainer = adContainer,
            priceFloor = priceFloor
        )
    }

    override fun interstitialParams(priceFloor: Double, lineItems: List<LineItem>): AdSource.AdParams {
        return AdmobFullscreenAdParams(
            admobLineItems = lineItems.filterByDemandId(),
            priceFloor = priceFloor
        )
    }

    override fun rewardedParams(priceFloor: Double, lineItems: List<LineItem>): AdSource.AdParams {
        return AdmobFullscreenAdParams(
            admobLineItems = lineItems.filterByDemandId(),
            priceFloor = priceFloor
        )
    }

    private fun List<LineItem>.filterByDemandId() =
        this.filter { it.demandId == demandId.demandId }.mapNotNull {
            val price = it.priceFloor ?: return@mapNotNull null
            val adUnitId = it.adUnitId ?: return@mapNotNull null
            AdmobLineItem(
                price = price,
                adUnitId = adUnitId
            )
        }.sortedBy { it.price }

    private fun InterstitialAd.setCoreListener(ownerDemandAd: DemandAd, auctionData: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(ownerDemandAd)
        this.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                coreListener.onAdClicked(auctionData.ad)
            }

            override fun onAdDismissedFullScreenContent() {
                coreListener.onAdHidden(auctionData.ad)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                coreListener.onAdDisplayFailed(adError.asBidonError())
            }

            override fun onAdShowedFullScreenContent() {
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(auctionData.ad)
                coreListener.onAdDisplayed(auctionData.ad)
            }

            override fun onAdImpression() {
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(auctionData.ad)
                coreListener.onAdImpression(auctionData.ad)
            }
        }
    }

    private fun RewardedAd.setCoreListener(ownerDemandAd: DemandAd, auctionData: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(ownerDemandAd)
        this.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                coreListener.onAdClicked(auctionData.ad)
            }

            override fun onAdDismissedFullScreenContent() {
                coreListener.onAdHidden(auctionData.ad)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                coreListener.onAdDisplayFailed(adError.asBidonError())
            }

            override fun onAdShowedFullScreenContent() {
                coreListener.onAdDisplayed(auctionData.ad)
            }

            override fun onAdImpression() {
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(auctionData.ad)
                coreListener.onAdImpression(auctionData.ad)
            }
        }
    }

    private fun AdView.setCoreListener(demandAd: DemandAd, auctionData: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(demandAd)
        this.adListener = object : AdListener() {
            override fun onAdClicked() {
                coreListener.onAdClicked(auctionData.ad)
            }

            override fun onAdClosed() {
                coreListener.onAdHidden(auctionData.ad)
            }

            override fun onAdOpened() {
                coreListener.onAdDisplayed(auctionData.ad)
            }

            override fun onAdImpression() {
                coreListener.onAdImpression(auctionData.ad)
                SdkCore.getAdRevenueInterceptor()?.onAdRevenueReceived(auctionData.ad)
            }
        }
    }

    private fun List<AdmobLineItem>.getPrice(unitId: String): Double {
        return this.mapNotNull { (price, adUnitId) ->
            price.takeIf { unitId == adUnitId }
        }.first()
    }

    private fun BannerSize.asAdmobAdSize() = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
        else -> null
    }

    private fun asAd(demandAd: DemandAd, sourceAd: Any, price: Double): Ad {
        return Ad(
            demandId = AdmobDemandId,
            demandAd = demandAd,
            price = price,
            sourceAd = sourceAd,
            monetizationNetwork = BNMediationNetwork.GoogleAdmob.networkName,
            dsp = null,
            auctionRound = Ad.AuctionRound.PostBid,
            currencyCode = null
        )
    }
}