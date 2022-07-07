package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.demands.*
import io.bidmachine.BidMachine
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val BidMachineDemandId = DemandId("bidmachine")

class BidMachineAdapter : Adapter.PostBid, AdSource.Interstitial {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId

    override suspend fun init(context: Context, configParams: Bundle): Unit = suspendCoroutine { continuation ->
        this.context = context
        val sourceId = configParams.getString(SourceIdKey) ?: "1" // TODO remove 1
        BidMachine.initialize(context, sourceId) {
            continuation.resume(Unit)
        }
    }

    override fun interstitial(activity: Activity?, demandAd: DemandAd, adParams: Bundle): AuctionRequest {
        return AuctionRequest { data ->
            suspendCancellableCoroutine { continuation ->
                val isFinished = AtomicBoolean(false)
                val interstitialRequest = InterstitialRequest.Builder().apply {
                    data?.let {
                        setPriceFloorParams(PriceFloorParams().addPriceFloor(it.priceFloor))
                    }
                }.build()
                InterstitialAd(context)
                    .setListener(object : InterstitialListener {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                val auctionResult = AuctionResult(
                                    ad = Ad(
                                        demandId = BidMachineDemandId,
                                        demandAd = demandAd,
                                        price = interstitialAd.auctionResult?.price ?: 0.0,
                                        sourceAd = interstitialAd
                                    ),
                                    adProvider = object : AdProvider {
                                        override fun canShow(): Boolean = interstitialAd.canShow()
                                        override fun showAd(activity: Activity?, adParams: Bundle) = interstitialAd.show()
                                        override fun destroy() = interstitialAd.destroy()
                                    }
                                )
                                interstitialAd.setCoreListener(auctionResult)
                                continuation.resume(Result.success(auctionResult))
                            }
                        }

                        override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                interstitialAd.setListener(null)
                                continuation.resume(Result.failure(bmError.asBidonError()))
                            }
                        }

                        override fun onAdShown(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdImpression(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClicked(interstitialAd: InterstitialAd) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdExpired(interstitialAd: InterstitialAd) {
                            if (!isFinished.getAndSet(true)) {
                                // remove listener
                                interstitialAd.setListener(null)
                                continuation.resume(Result.failure(DemandError.Expired))
                            }
                        }

                        override fun onAdShowFailed(interstitialAd: InterstitialAd, p1: BMError) {
                            error("unexpected state. remove on release a28.")
                        }

                        override fun onAdClosed(interstitialAd: InterstitialAd, p1: Boolean) {
                            error("unexpected state. remove on release a28.")
                        }
                    }).load(interstitialRequest)
            }
        }
    }

    private fun InterstitialAd.setCoreListener(auctionResult: AuctionResult) {
        val coreListener = SdkCore.getListenerForDemand(auctionResult.ad.demandAd)
        this.setListener(
            object : InterstitialListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdLoadFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdShown(ad: InterstitialAd) {
                    coreListener.onAdDisplayed(auctionResult.ad)
                }

                override fun onAdImpression(ad: InterstitialAd) {
                    coreListener.onAdLoaded(auctionResult.ad)
                }

                override fun onAdClicked(ad: InterstitialAd) {
                    coreListener.onAdClicked(auctionResult.ad)
                }

                override fun onAdExpired(ad: InterstitialAd) {
                }

                override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
                    coreListener.onAdDisplayFailed(bmError.asBidonError())
                }

                override fun onAdClosed(ad: InterstitialAd, bmError: Boolean) {
                    coreListener.onAdHidden(auctionResult.ad)
                }
            }
        )
    }

}

private const val SourceIdKey = "SourceId"