package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.applovin.ext.asBidonError
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

val ApplovinMaxDemandId = DemandId("applovin")

class ApplovinMaxAdapter : Adapter.Mediation,
    AdSource.Interstitial, AdSource.Rewarded,
    AdRevenueSource, ExtrasSource {
    private val adRevenueListeners = mutableMapOf<DemandAd, AdRevenueListener>()
    private val extras = mutableMapOf<DemandAd, Bundle>()

    override val demandId: DemandId = ApplovinMaxDemandId

    override suspend fun init(context: Context, configParams: Bundle) {
        require(AppLovinSdk.getInstance(context).isInitialized)
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity) }
        val adUnitId = adParams.getString(adUnitIdKey)
        val maxInterstitialAd = MaxInterstitialAd(adUnitId, activity)

        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                maxInterstitialAd.setListener(
                    object : MaxAdListener {
                        override fun onAdLoaded(maxAd: MaxAd) {
                            if (!isFinished.getAndSet(true)) {
                                val ad = Ad(
                                    demandId = ApplovinMaxDemandId,
                                    demandAd = demandAd,
                                    price = maxAd.revenue,
                                    sourceAd = maxAd
                                )
                                val auctionResult = AuctionResult(
                                    ad = ad,
                                    adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider {
                                        override fun canShow(): Boolean {
                                            return maxInterstitialAd.isReady
                                        }

                                        override fun showAd(activity: Activity?, adParams: Bundle) {
                                            val placement = adParams.getString(placementKey)
                                            val customData = adParams.getString(customDataKey)
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
                                adRevenueListeners[demandAd]?.let { adRevenueListener ->
                                    maxInterstitialAd.setRevenueListener {
                                        adRevenueListener.onAdRevenuePaid(ad)
                                    }
                                }
                                extras[demandAd]?.let { bundle ->
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

                        override fun onAdDisplayed(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdHidden(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(ad: MaxAd?) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                            error("unexpected state. remove on release a28.")
                        }
                    }
                )
                maxInterstitialAd.loadAd()
            }
        }
    }

    override fun setAdRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        adRevenueListeners[demandAd] = adRevenueListener
    }

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
        extras[demandAd] = adParams
    }

    override fun rewarded(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        if (activity == null) return AuctionRequest { Result.failure(DemandError.NoActivity) }
        val adUnitId = adParams.getString(adUnitIdKey)
        val rewardedAd = MaxRewardedAd.getInstance(adUnitId, activity)
        return AuctionRequest {
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                rewardedAd.setListener(object : MaxRewardedAdListener {
                    override fun onAdLoaded(maxAd: MaxAd) {
                        if (!isFinished.getAndSet(true)) {
                            val ad = Ad(
                                demandId = ApplovinMaxDemandId,
                                demandAd = demandAd,
                                price = maxAd.revenue,
                                sourceAd = maxAd
                            )
                            val auctionResult = AuctionResult(
                                ad = ad,
                                adProvider = object : AdProvider, AdRevenueProvider, ExtrasProvider {
                                    override fun canShow(): Boolean = rewardedAd.isReady

                                    override fun showAd(activity: Activity?, adParams: Bundle) {
                                        val placement = adParams.getString(placementKey)
                                        val customData = adParams.getString(customDataKey)
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
                            adRevenueListeners[demandAd]?.let { adRevenueListener ->
                                rewardedAd.setRevenueListener {
                                    adRevenueListener.onAdRevenuePaid(ad)
                                }
                            }
                            extras[demandAd]?.let { bundle ->
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

                    override fun onAdDisplayed(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdHidden(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdClicked(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onRewardedVideoStarted(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onRewardedVideoCompleted(ad: MaxAd?) {
                        error("unexpected state. remove on release a28.")
                    }

                    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                        error("unexpected state. remove on release a28.")
                    }
                })
                rewardedAd.loadAd()
            }
        }
    }


    private fun MaxInterstitialAd.setCoreListener(auctionResult: AuctionResult) {
        val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdDisplayed(ad: MaxAd?) {
                    core.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdHidden(ad: MaxAd?) {
                    core.onAdHidden(auctionResult.ad)
                }

                override fun onAdClicked(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                    core.onAdLoadFailed(error.asBidonError())
                }

                override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                    core.onAdDisplayFailed(error.asBidonError())
                }
            }
        )
    }

    private fun MaxRewardedAd.setCoreListener(auctionResult: AuctionResult) {
        val core = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : MaxRewardedAdListener {
                override fun onAdLoaded(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdDisplayed(ad: MaxAd?) {
                    core.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdHidden(ad: MaxAd?) {
                    core.onAdHidden(auctionResult.ad)
                }

                override fun onAdClicked(ad: MaxAd?) {
                    core.onAdClicked(auctionResult.ad)
                }

                override fun onAdLoadFailed(adUnitId: String?, error: MaxError) {
                    core.onAdLoadFailed(error.asBidonError())
                }

                override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError) {
                    core.onAdDisplayFailed(error.asBidonError())
                }

                override fun onRewardedVideoStarted(ad: MaxAd?) {
                    core.onRewardedStarted(auctionResult.ad)
                }

                override fun onRewardedVideoCompleted(ad: MaxAd?) {
                    core.onRewardedCompleted(auctionResult.ad)
                }

                override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                    core.onUserRewarded(
                        auctionResult.ad, reward?.let {
                            RewardedAdListener.Reward(
                                label = reward.label ?: "",
                                amount = reward.amount
                            )
                        }
                    )
                }
            }
        )
    }

}

internal const val adUnitIdKey = "adUnitId"
internal const val placementKey = "placement"
internal const val customDataKey = "customData"
internal const val keyKey = "key"
internal const val valueKey = "valueKey"