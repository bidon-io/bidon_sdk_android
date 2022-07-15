package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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

class AdmobAdapter : Adapter.PostBid<AdmobParameters>,
    AdSource.Interstitial, AdSource.Rewarded, AdSource.Banner {
    private lateinit var context: Context

    override val demandId = AdmobDemandId

    private val bannersAdUnits = mutableMapOf<Double, AdUnitId>()
    private val interstitialAdUnits = mutableMapOf<Double, AdUnitId>()
    private val rewardedAdUnits = mutableMapOf<Double, AdUnitId>()

    override suspend fun init(activity: Activity, configParams: AdmobParameters): Unit = suspendCoroutine { continuation ->
        this.context = activity.applicationContext
        /**
         * Don't forget set Automatic refresh is Disabled for each AdUnit.
         * Manage refresh rate with [AutoRefresher.setAutoRefresh].
         */
        interstitialAdUnits.putAll(configParams.interstitials.map { (price, adUnit) ->
            price to AdUnitId(adUnit)
        })
        rewardedAdUnits.putAll(configParams.rewarded.map { (price, adUnit) ->
            price to AdUnitId(adUnit)
        })
        bannersAdUnits.putAll(configParams.banners.map { (price, adUnit) ->
            price to AdUnitId(adUnit)
        })
        MobileAds.initialize(context) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest { data ->
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    InterstitialAd.load(
                        context,
                        interstitialAdUnits.getUnitId(data?.priceFloor ?: 0.0).value,
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
        return AuctionRequest { data ->
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    RewardedAd.load(
                        context,
                        rewardedAdUnits.getUnitId(data?.priceFloor ?: 0.0).value,
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

    override fun banner(context: Context, demandAd: DemandAd, adParams: Bundle, adContainer: ViewGroup?): AuctionRequest {
        return AuctionRequest { data ->
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val isFinished = AtomicBoolean(false)
                    val adRequest = AdRequest.Builder().build()
                    val adView = AdView(context)
                    val bannerSize = adParams.getInt(BannerSizeKey, BannerSize.Banner.ordinal).let {
                        BannerSize.values()[it]
                    }
                    adView.setAdSize(bannerSize.asAdmobAdSize())
                    adView.adUnitId = bannersAdUnits.getUnitId(data?.priceFloor ?: 0.0).value
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
        }
    }

    private fun Map<Double, AdUnitId>.getUnitId(price: Double): AdUnitId {
        val sortedKeys = this.keys.sorted()
        val priceFloor = sortedKeys.firstOrNull { it > price } ?: sortedKeys.last()
        return this.getValue(priceFloor)
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
        else -> error("Not supported")
    }
}