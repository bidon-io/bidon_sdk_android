package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMFullscreenAuctionParams
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.data.models.stats.asRoundStatus
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.AdState
import com.appodealstack.bidon.domain.adapter.WinLossNotifiable
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.*
import com.appodealstack.bidon.domain.common.ext.asFailure
import com.appodealstack.bidon.domain.common.ext.asSuccess
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.domain.stats.impl.logInternal
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

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)
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
                logInternal(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                markBidFinished(
                    ecpm = result.price,
                    roundStatus = RoundStatus.Successful,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = result.price,
                            adSource = this@BMInterstitialAdImpl,
                        )
                    )
                )
            }

            override fun onRequestFailed(request: InterstitialRequest, bmError: BMError) {
                logInternal(Tag, "onRequestFailed $bmError. $this", bmError.asBidonError(demandId))
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = bmError.asBidonError(demandId).asRoundStatus(),
                )
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onRequestExpired(request: InterstitialRequest) {
                logInternal(Tag, "onRequestExpired: $this")
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = RoundStatus.NoBid,
                )
                adState.tryEmit(AdState.LoadFailed(BidonError.Expired(demandId)))
            }
        }
    }

    private val interstitialListener by lazy {
        object : InterstitialListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInternal(Tag, "onAdLoaded: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.Fill(interstitialAd.asAd()))
            }

            override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                logInternal(Tag, "onAdLoadFailed: $this", bmError.asBidonError(demandId))
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
                logInternal(Tag, "onAdShowFailed: $this", bmError.asBidonError(demandId))
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.ShowFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdImpression(interstitialAd: InterstitialAd) {
                logInternal(Tag, "onAdShown: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.Impression(interstitialAd.asAd()))
                adState.tryEmit(AdState.PaidRevenue(interstitialAd.asAd()))
            }

            override fun onAdClicked(interstitialAd: InterstitialAd) {
                logInternal(Tag, "onAdClicked: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.Clicked(interstitialAd.asAd()))
            }

            override fun onAdExpired(interstitialAd: InterstitialAd) {
                logInternal(Tag, "onAdExpired: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.Expired(interstitialAd.asAd()))
            }

            override fun onAdClosed(interstitialAd: InterstitialAd, boolean: Boolean) {
                logInternal(Tag, "onAdClosed: $this")
                this@BMInterstitialAdImpl.interstitialAd = interstitialAd
                adState.tryEmit(AdState.Closed(interstitialAd.asAd()))
            }
        }
    }

    override suspend fun bid(adParams: BMFullscreenAuctionParams): AuctionResult {
        logInternal(Tag, "Starting with $adParams: $this")
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
        val state = adState.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdState.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> {
        logInternal(Tag, "Starting fill: $this")
        markFillStarted()
        val context = context
        if (context == null) {
            adState.tryEmit(AdState.LoadFailed(BidonError.NoContextFound))
        } else {
            val bmInterstitialAd = InterstitialAd(context).also {
                interstitialAd = it
            }
            bmInterstitialAd.setListener(interstitialListener)
            bmInterstitialAd.load(adRequest)
        }
        val state = adState.first {
            it is AdState.Fill || it is AdState.LoadFailed || it is AdState.Expired
        }
        return when (state) {
            is AdState.Fill -> {
                markFillFinished(RoundStatus.Successful)
                state.ad.asSuccess()
            }
            is AdState.LoadFailed -> {
                markFillFinished(RoundStatus.NoFill)
                state.cause.asFailure()
            }
            is AdState.Expired -> {
                markFillFinished(RoundStatus.NoFill)
                BidonError.FillTimedOut(demandId).asFailure()
            }
            else -> error("unexpected: $state")
        }
    }

    override fun show(activity: Activity) {
        logInternal(Tag, "Starting show: $this")
        if (interstitialAd?.canShow() == true) {
            interstitialAd?.show()
        } else {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
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
        logInternal(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        interstitialAd?.destroy()
        interstitialAd = null
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = this.auctionResult?.price ?: 0.0,
            sourceAd = this,
            currencyCode = "USD",
            roundId = roundId,
            dsp = this.auctionResult?.demandSource,
            monetizationNetwork = demandId.demandId,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "BidMachine Interstitial"
