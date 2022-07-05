package com.appodealstack.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.demands.ObjRequest
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.DemandId
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val AdmobDemandId = DemandId("admob")

@JvmInline
private value class AdUnitId(val value: String)

class AdmobDemand : Demand.PostBid {
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

    override fun createActionRequest(ownerDemandAd: DemandAd): AuctionRequest.PostBid {
        return object : AuctionRequest.PostBid {
            override suspend fun execute(additionalData: AuctionRequest.AdditionalData?): AuctionData {
                return executeRequest(additionalData, ownerDemandAd)
            }
        }
    }

    private suspend fun executeRequest(additionalData: AuctionRequest.AdditionalData?, ownerDemandAd: DemandAd): AuctionData =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(context, getUnitId().value, adRequest, object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        if (!isFinished.getAndSet(true)) {
                            continuation.resume(
                                AuctionData.Failure(
                                    adType = ownerDemandAd.adType,
                                    objRequest = Unit,
                                    cause = loadAdError.asBidonError(),
                                    demandId = demandId
                                )
                            )
                        }
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        if (!isFinished.getAndSet(true)) {
                            val auctionData = AuctionData.Success(
                                demandId = demandId,
                                objRequest = createObjRequest(interstitialAd),
                                objResponse = interstitialAd,
                                adType = ownerDemandAd.adType,
                                price = adUnits.mapNotNull { (price, adUnitId) ->
                                    if (interstitialAd.adUnitId == adUnitId.value) {
                                        price
                                    } else {
                                        null
                                    }
                                }.first()
                            )
                            interstitialAd.setCoreListener(ownerDemandAd, auctionData)
                            continuation.resume(auctionData)
                        }
                    }
                })
            }
        }

    private fun InterstitialAd.setCoreListener(ownerDemandAd: DemandAd, auctionData: AuctionData.Success) {
        val coreListener = SdkCore.getListenerForDemand(ownerDemandAd)
        this.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                coreListener.onAdClicked(auctionData)
            }

            override fun onAdDismissedFullScreenContent() {
                coreListener.onAdHidden(auctionData)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                coreListener.onAdDisplayFailed(
                    AuctionData.Failure(
                        demandId = demandId,
                        cause = adError.asBidonError(),
                        objRequest = auctionData.objRequest,
                        adType = auctionData.adType
                    )
                )
            }

            override fun onAdShowedFullScreenContent() {
                coreListener.onAdDisplayed(auctionData)
            }
        }
    }

    private fun createObjRequest(interstitialAd: InterstitialAd): ObjRequest {
        return object : ObjRequest {
            override fun canShowAd(): Boolean {
                return true
            }

            override fun showAd(activity: Activity?, adParams: Bundle) {
                if (activity == null) {
                    logInternal("AdmobDemand", "Error while showing InterstitialAd: activity is null.")
                } else {
                    interstitialAd.show(activity)
                }
            }
        }
    }

    private fun getUnitId(): AdUnitId {
        return adUnits.maxBy { it.key }.value
    }
}

private const val AdUnitIdKey = "AdUnitIdKey"
private const val PriceKey = "PriceKey"