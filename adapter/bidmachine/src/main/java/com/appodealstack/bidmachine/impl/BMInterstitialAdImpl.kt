package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenAuctionParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdEvent
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.adapter.DemandId
import com.appodealstack.bidon.adapter.WinLossNotifiable
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.config.BidonError
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.stats.models.RoundStatus
import com.appodealstack.bidon.stats.models.asRoundStatus
import com.appodealstack.bidon.utils.ext.asFailure
import com.appodealstack.bidon.utils.ext.asSuccess
import io.bidmachine.AdContentType
import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialRequest
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

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
        demandId = demandId
    ) {

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
    override val ad: Ad? get() = interstitialAd?.asAd()

    private var context: Context? = null
    private var adRequest: InterstitialRequest? = null
    private var interstitialAd: InterstitialAd? = null

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<InterstitialRequest> {
            override fun onRequestSuccess(
                request: InterstitialRequest,
                result: BMAuctionResult
            ) {
                logInfo(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                markBidFinished(
                    ecpm = result.price,
                    roundStatus = RoundStatus.Successful,
                )
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = result.price,
                            adSource = this@BMInterstitialAdImpl,
                        )
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                logError(Tag, "onRequestFailed $bmError. $this", bmError.asBidonError(demandId))
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = bmError.asBidonError(demandId).asRoundStatus(),
                )
                adEvent.tryEmit(AdEvent.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                logInfo(Tag, "onRequestExpired: $this")
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = RoundStatus.NoBid,
                )
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
                logError(Tag, "onAdLoadFailed: $this", bmError.asBidonError(demandId))
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                logError(Tag, "onAdShowFailed: $this", bmError.asBidonError(demandId))
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.ShowFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdShown: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adEvent.tryEmit(AdEvent.Shown(interstitialAd.asAd()))
                adEvent.tryEmit(AdEvent.PaidRevenue(interstitialAd.asAd()))
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

    override suspend fun bid(adParams: BMFullscreenAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted()
        context = adParams.context
        InterstitialRequest.Builder()
            .setAdContentType(AdContentType.All)
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.priceFloor))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(requestListener)
            .setPlacementId(demandAd.placement)
            .build()
            .also {
                adRequest = it
            }
            .request(adParams.context)
        val state = adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed
        }
        return when (state) {
            is AdEvent.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdEvent.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> {
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
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
        val state = adEvent.first {
            it is AdEvent.Fill || it is AdEvent.LoadFailed || it is AdEvent.Expired
        }
        return when (state) {
            is AdEvent.Fill -> {
                markFillFinished(RoundStatus.Successful)
                state.ad.asSuccess()
            }
            is AdEvent.LoadFailed -> {
                markFillFinished(RoundStatus.NoFill)
                state.cause.asFailure()
            }
            is AdEvent.Expired -> {
                markFillFinished(RoundStatus.NoFill)
                BidonError.FillTimedOut(demandId).asFailure()
            }
            else -> error("unexpected: $state")
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

    override fun notifyLoss() {
        adRequest?.notifyMediationLoss()
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> {
        return BMFullscreenAuctionParams(
            priceFloor = priceFloor,
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
            price = this.auctionResult?.price ?: 0.0,
            sourceAd = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            networkName = demandId.demandId,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "BidMachine Interstitial"
