package org.bidon.bidmachine.impl

import android.app.Activity
import android.content.Context
import io.bidmachine.AdContentType
import io.bidmachine.AdRequest
import io.bidmachine.BidMachine
import io.bidmachine.CustomParams
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

internal class BMInterstitialAdImpl :
    AdSource.Interstitial<BMFullscreenAuctionParams>,
    AdLoadingType.Bidding<BMFullscreenAuctionParams>,
    AdLoadingType.Network<BMFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: InterstitialRequest? = null
    private var interstitialAd: InterstitialAd? = null
    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.canShow() == true

    private var isBiddingRequest = true
    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<InterstitialRequest> {
            override fun onRequestSuccess(
                request: InterstitialRequest,
                result: BMAuctionResult
            ) {
                logInfo(TAG, "onRequestSuccess $result: $this")
                adRequest = request
                when (isBiddingRequest) {
                    false -> {
                        fillRequest(request)
                    }

                    true -> {
                        emitEvent(
                            AdEvent.Bid(
                                AuctionResult.Network(
                                    adSource = this@BMInterstitialAdImpl,
                                    roundStatus = RoundStatus.Successful
                                )
                            )
                        )
                    }
                }
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                val error = bmError.asBidonErrorOnBid(demandId)
                logError(TAG, "onRequestFailed $bmError. $this", error)
                adRequest = request
                emitEvent(AdEvent.LoadFailed(error))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                logInfo(TAG, "onRequestExpired: $this")
                adRequest = request
                emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }
        }
    }

    override fun getToken(context: Context): String = BidMachine.getBidToken(context)

    override fun adRequest(adParams: BMFullscreenAuctionParams) {
        isBiddingRequest = true
        request(adParams, requestListener)
    }

    /**
     * As Bidding Network
     */
    override fun fill() {
        isBiddingRequest = true
        fillRequest(adRequest)
    }

    /**
     * As AdNetwork
     */
    override fun fill(adParams: BMFullscreenAuctionParams) {
        isBiddingRequest = false
        request(adParams, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                payload = json?.optString("payload")
            )
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        interstitialAd?.destroy()
        interstitialAd = null
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    private fun request(adParams: BMFullscreenAuctionParams, requestListener: AdRequest.AdRequestListener<InterstitialRequest>) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        val requestBuilder = InterstitialRequest.Builder()
            .setAdContentType(AdContentType.All)
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setBidPayload(adParams.payload)
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(requestListener)
        adParams.payload?.let {
            requestBuilder.setBidPayload(it)
        }
        requestBuilder.build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
    }

    private fun fillRequest(adRequest: InterstitialRequest?) {
        logInfo(TAG, "Starting fill: $this")
        val context = context
        if (context == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            interstitialAd = InterstitialAd(context)
            val interstitialListener = object : InterstitialListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdLoaded: $this")
                    emitEvent(AdEvent.Fill(interstitialAd.asAd()))
                }

                override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdLoadFailed: $this", error)
                    emitEvent(AdEvent.LoadFailed(error))
                }

                override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdShowFailed: $this", error)
                    emitEvent(AdEvent.ShowFailed(error))
                }

                override fun onAdImpression(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdShown: $this")
                    emitEvent(AdEvent.Shown(interstitialAd.asAd()))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = interstitialAd.asAd(),
                            adValue = interstitialAd.auctionResult.asBidonAdValue()
                        )
                    )
                }

                override fun onAdClicked(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdClicked: $this")
                    emitEvent(AdEvent.Clicked(interstitialAd.asAd()))
                }

                override fun onAdExpired(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdExpired: $this")
                    emitEvent(AdEvent.Expired(interstitialAd.asAd()))
                }

                override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                    logInfo(TAG, "onAdClosed: $this")
                    emitEvent(AdEvent.Closed(interstitialAd.asAd()))
                }
            }
            interstitialAd
                ?.setListener(interstitialListener)
                ?.load(adRequest)
        }
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = this.auctionResult?.price ?: 0.0,
            demandAdObject = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            networkName = demandId.demandId,
            auctionId = auctionId,
            adUnitId = null
        )
    }
}

private const val TAG = "BidMachineInterstitial"
