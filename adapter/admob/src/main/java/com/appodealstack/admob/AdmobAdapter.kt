package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@JvmInline
private value class AdUnitId(val value: String)

class AdmobAdapter : Adapter.PostBid, AdSource.Interstitial {
    private lateinit var context: Context

    override val demandId = AdmobDemandId

    private val adUnits = mutableMapOf<Double, AdUnitId>()

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        this.context = context
        val adUnitId = configParams.getString(AdUnitIdKey)
            ?: "ca-app-pub-3940256099942544/1033173712" // TODO remove "ca-app-pub-3940256099942544/1033173712"
        val price = configParams.getDouble(PriceKey, 0.14)
        adUnits[price] = AdUnitId(adUnitId)
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
                                        price = adUnits.mapNotNull { (price, adUnitId) ->
                                            if (interstitialAd.adUnitId == adUnitId.value) {
                                                price
                                            } else {
                                                null
                                            }
                                        }.first(),
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

    private fun getUnitId(): AdUnitId {
        return adUnits.maxBy { it.key }.value
    }
}

private const val AdUnitIdKey = "AdUnitIdKey"
private const val PriceKey = "PriceKey"