package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.banners.BannerSize
import com.appodealstack.mads.demands.banners.BannerSizeKey
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@JvmInline
private value class AdUnitId(val value: String)

class AdmobAdapter : Adapter.PostBid,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner {
    private lateinit var context: Context

    override val demandId = AdmobDemandId

    private val bannersAdUnits = mutableMapOf<Double, AdUnitId>()
    private val interstitialAdUnits = mutableMapOf<Double, AdUnitId>()
    private val rewardedAdUnits = mutableMapOf<Double, AdUnitId>()

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        this.context = context
        val adUnitId = configParams.getString(AdUnitIdKey)
            ?: "ca-app-pub-3940256099942544/1033173712" // TODO remove "ca-app-pub-3940256099942544/1033173712"
        val price = configParams.getDouble(PriceKey, 0.14)
        interstitialAdUnits[price] = AdUnitId(adUnitId)
        rewardedAdUnits[price] = AdUnitId("ca-app-pub-3940256099942544/5224354917")
        bannersAdUnits[price] = AdUnitId("ca-app-pub-3940256099942544/6300978111")
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    InterstitialAd.load(context, getUnitId().value, adRequest, object : InterstitialAdLoadCallback() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            if (!isFinished.getAndSet(true)) {
                                continuation.resume(Result.failure(loadAdError.asBidonError()))
                            }
                        }

                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                val auctionResult = AuctionResult(
                                    ad = Ad(
                                        demandId = AdmobDemandId,
                                        demandAd = demandAd,
                                        price = interstitialAdUnits.getPrice(unitId = interstitialAd.adUnitId),
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

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    // todo remove "ca-app-pub-3940256099942544/5224354917"
                    RewardedAd.load(
                        context,
                        "ca-app-pub-3940256099942544/5224354917" ?: getUnitId().value,
                        adRequest,
                        object : RewardedAdLoadCallback() {
                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                if (!isFinished.getAndSet(true)) {
                                    continuation.resume(Result.failure(loadAdError.asBidonError()))
                                }
                            }

                            override fun onAdLoaded(rewardedAd: RewardedAd) {
                                if (!isFinished.getAndSet(true)) {
                                    val ad = Ad(
                                        demandId = AdmobDemandId,
                                        demandAd = demandAd,
                                        price = rewardedAdUnits.getPrice(unitId = rewardedAd.adUnitId),
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

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    val adView = AdView(context)
                    val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
                        BannerSize.values()[it]
                    }
                    adView.setAdSize(bannerSize.asAdmobAdSize())
                    adView.adUnitId = bannersAdUnits.values.first().value
                    adView.adListener = object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            if (!isFinished.getAndSet(true)) {
                                continuation.resume(Result.failure(loadAdError.asBidonError()))
                            }
                        }

                        override fun onAdLoaded() {
                            val ad = Ad(
                                demandId = AdmobDemandId,
                                demandAd = demandAd,
                                price = bannersAdUnits.getPrice(unitId = adView.adUnitId),
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
            }
        }
    }

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
                coreListener.onAdDisplayed(auctionData.ad)
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
        }
    }

    private fun AdView.setCoreListener(ownerDemandAd: DemandAd, auctionData: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(ownerDemandAd)
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
        }
    }

    private fun getUnitId(): AdUnitId {
        return interstitialAdUnits.maxBy { it.key }.value
    }

    private fun Map<Double, AdUnitId>.getPrice(unitId: String): Double {
        return this.mapNotNull { (price, adUnitId) ->
            price.takeIf { unitId == adUnitId.value }
        }.first()
    }

    private fun BannerSize.asAdmobAdSize() = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
    }
}

private const val AdUnitIdKey = "AdUnitIdKey"
private const val PriceKey = "PriceKey"