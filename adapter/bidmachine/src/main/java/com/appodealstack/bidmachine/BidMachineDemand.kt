package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.ObjRequest
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.DemandError
import com.appodealstack.mads.demands.DemandId
import io.bidmachine.BidMachine
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val BidMachineDemandId = DemandId("bidmachine")

class BidMachineDemand : Demand.PostBid {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        this.context = context
        val sourceId = configParams.getString(SourceIdKey) ?: "1" // TODO remove 1
        BidMachine.initialize(context, sourceId) {
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
        suspendCoroutine { continuation ->
            val isFinished = AtomicBoolean(false)
            val interstitialRequest = InterstitialRequest.Builder().apply {
                additionalData?.let {
                    setPriceFloorParams(PriceFloorParams().addPriceFloor(it.priceFloor))
                }
            }.build()
            InterstitialAd(context)
                .setListener(object : InterstitialListener {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        if (!isFinished.getAndSet(true)) {
                            setCoreListener(interstitialAd, interstitialAd.auctionResult?.price ?: 0.0, ownerDemandAd)
                            continuation.resume(
                                AuctionData.Success(
                                    demandId = demandId,
                                    price = interstitialAd.auctionResult?.price ?: 0.0,
                                    objRequest = createObjRequest(interstitialAd),
                                    objResponse = interstitialAd,
                                    adType = AdType.Interstitial,
                                )
                            )
                        }
                    }

                    override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                        if (!isFinished.getAndSet(true)) {
                            val failure = AuctionData.Failure(
                                demandId = demandId,
                                adType = AdType.Interstitial,
                                objRequest = interstitialAd,
                                cause = bmError.asBidonError()
                            )
                            // remove listener
                            interstitialAd.setListener(null)
                            continuation.resume(failure)
                        }
                    }

                    override fun onAdShown(interstitialAd: InterstitialAd) {
                    }

                    override fun onAdImpression(interstitialAd: InterstitialAd) {
                    }

                    override fun onAdClicked(interstitialAd: InterstitialAd) {
                    }

                    override fun onAdExpired(interstitialAd: InterstitialAd) {
                        if (!isFinished.getAndSet(true)) {
                            val failure = AuctionData.Failure(
                                demandId = demandId,
                                adType = AdType.Interstitial,
                                objRequest = interstitialAd,
                                cause = DemandError.Expired
                            )
                            // remove listener
                            interstitialAd.setListener(null)
                            continuation.resume(failure)
                        }
                    }

                    override fun onAdShowFailed(interstitialAd: InterstitialAd, p1: BMError) {
                    }

                    override fun onAdClosed(interstitialAd: InterstitialAd, p1: Boolean) {
                    }

                }).load(interstitialRequest)
        }

    private fun setCoreListener(interstitialAd: InterstitialAd, price: Double, ownerDemandAd: DemandAd) {
        val coreListener = SdkCore.getListenerForDemand(ownerDemandAd)
        val success = AuctionData.Success(
            objRequest = createObjRequest(interstitialAd),
            demandId = demandId,
            adType = ownerDemandAd.adType,
            objResponse = interstitialAd,
            price = price
        )
        interstitialAd.setListener(
            object : InterstitialListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    coreListener.onAdLoaded(success)
                }

                override fun onAdLoadFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(
                        AuctionData.Failure(
                            objRequest = createObjRequest(ad),
                            demandId = demandId,
                            adType = ownerDemandAd.adType,
                            cause = bmError.asBidonError()
                        )
                    )
                }

                override fun onAdShown(ad: InterstitialAd) {
                    coreListener.onAdDisplayed(success)
                }

                override fun onAdImpression(ad: InterstitialAd) {
                    coreListener.onAdLoaded(success)
                }

                override fun onAdClicked(ad: InterstitialAd) {
                    coreListener.onAdClicked(success)
                }

                override fun onAdExpired(ad: InterstitialAd) {
                }

                override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(
                        AuctionData.Failure(
                            objRequest = createObjRequest(ad),
                            demandId = demandId,
                            adType = AdType.Interstitial,
                            cause = bmError.asBidonError()
                        )
                    )
                }

                override fun onAdClosed(ad: InterstitialAd, bmError: Boolean) {
                    coreListener.onAdHidden(success)
                }
            }
        )
    }

    private fun createObjRequest(interstitialAd: InterstitialAd): ObjRequest = object : ObjRequest {
        override fun canShowAd(): Boolean {
            return interstitialAd.canShow()
        }

        override fun showAd(activity: Activity?, adParams: Bundle) {
            interstitialAd.show()
        }
    }
}

private const val SourceIdKey = "SourceId"