package org.bidon.bidmachine.impl

import android.app.Activity
import android.content.Context
import io.bidmachine.AdContentType
import io.bidmachine.AdRequest
import io.bidmachine.CustomParams
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.ext.asSuccess

internal class BMInterstitialAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Interstitial<BMFullscreenAuctionParams>,
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val ad: Ad? get() = interstitialAd?.asAd()

    private var context: Context? = null
    private var adRequest: InterstitialRequest? = null
    private var interstitialAd: InterstitialAd? = null
    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.canShow() == true

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<InterstitialRequest> {
            override fun onRequestSuccess(
                request: InterstitialRequest,
                result: BMAuctionResult
            ) {
                logInfo(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = result.price,
                            adSource = this@BMInterstitialAdImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                val error = bmError.asBidonErrorOnBid(demandId)
                logError(Tag, "onRequestFailed $bmError. $this", error)
                adRequest = request
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                logInfo(Tag, "onRequestExpired: $this")
                adRequest = request
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }
        }
    }

    private val interstitialListener by lazy {
        object : InterstitialListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdLoaded: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Fill(interstitialAd.asAd()))
            }

            override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(Tag, "onAdLoadFailed: $this", error)
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(Tag, "onAdShowFailed: $this", error)
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.ShowFailed(error))
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdShown: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Shown(interstitialAd.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = interstitialAd.asAd(),
                        adValue = interstitialAd.auctionResult.asBidonAdValue()
                    )
                )
            }

            override fun onAdClicked(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdClicked: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Clicked(interstitialAd.asAd()))
            }

            override fun onAdExpired(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdExpired: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Expired(interstitialAd.asAd()))
            }

            override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                logInfo(Tag, "onAdClosed: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Closed(interstitialAd.asAd()))
            }
        }
    }

    override fun bid(adParams: BMFullscreenAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        context = adParams.context
        InterstitialRequest.Builder()
            .setAdContentType(AdContentType.All)
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.pricefloor))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(requestListener)
            .build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
    }

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        val context = context
        if (context == null) {
            adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            val bmInterstitialAd = InterstitialAd(context).also {
                interstitialAd = it
            }
            bmInterstitialAd.setListener(interstitialListener)
            bmInterstitialAd.load(adRequest)
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> {
        return BMFullscreenAuctionParams(
            pricefloor = pricefloor,
            timeout = timeout,
            context = activity.applicationContext
        ).asSuccess()
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        interstitialAd?.destroy()
        interstitialAd = null
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

private const val Tag = "BidMachine Interstitial"
