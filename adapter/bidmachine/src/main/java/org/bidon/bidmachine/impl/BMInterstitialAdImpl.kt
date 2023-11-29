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
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class BMInterstitialAdImpl :
    AdSource.Interstitial<BMFullscreenAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: InterstitialRequest? = null
    private var interstitialAd: InterstitialAd? = null
    private var isBidding = false

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.canShow() == true

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String {
        isBidding = true
        return BidMachine.getBidToken(context)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                payload = json?.optString("payload")
            )
        }
    }

    override fun load(adParams: BMFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        val requestBuilder = InterstitialRequest.Builder()
            .apply {
                if (bidType == BidType.CPM) {
                    this.setNetworks("")
                }
            }
            .setAdContentType(AdContentType.All)
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setBidPayload(adParams.payload)
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(
                object : AdRequest.AdRequestListener<InterstitialRequest> {
                    override fun onRequestSuccess(
                        request: InterstitialRequest,
                        result: BMAuctionResult
                    ) {
                        logInfo(TAG, "onRequestSuccess $result: $this")
                        fillRequest(request)
                    }

                    override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                        logInfo(TAG, "onRequestFailed $bmError. $this")
                        emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                    }

                    override fun onRequestExpired(request: InterstitialRequest) {
                        logInfo(TAG, "onRequestExpired: $this")
                        emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
                    }
                }
            )
        adParams.payload?.let {
            requestBuilder.setBidPayload(it)
        }
        requestBuilder.build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        interstitialAd?.destroy()
        interstitialAd = null
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
                    setDsp(interstitialAd.auctionResult?.demandSource)
                    if (!isBidding) {
                        setPrice(interstitialAd.auctionResult?.price ?: 0.0)
                    }
                    getAd()?.let {
                        emitEvent(AdEvent.Fill(it))
                    }
                }

                override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdLoadFailed: $this", error)
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                    val error = bmError.asBidonErrorOnFill(demandId)
                    logError(TAG, "onAdShowFailed: $this", error)
                    emitEvent(AdEvent.ShowFailed(error))
                }

                override fun onAdImpression(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdShown: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Shown(it))
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = it,
                                adValue = interstitialAd.auctionResult.asBidonAdValue()
                            )
                        )
                    }
                }

                override fun onAdClicked(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdClicked: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Clicked(it))
                    }
                }

                override fun onAdExpired(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdExpired: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Expired(it))
                    }
                }

                override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                    logInfo(TAG, "onAdClosed: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Closed(it))
                    }
                    this@BMInterstitialAdImpl.interstitialAd = null
                    this@BMInterstitialAdImpl.adRequest = null
                }
            }
            interstitialAd
                ?.setListener(interstitialListener)
                ?.load(adRequest)
        }
    }
}

private const val TAG = "BidMachineInterstitial"
