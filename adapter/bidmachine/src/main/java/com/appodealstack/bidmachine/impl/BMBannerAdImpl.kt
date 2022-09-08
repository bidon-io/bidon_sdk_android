package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMBannerAuctionParams
import com.appodealstack.bidmachine.BidMachineBannerSize
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
import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class BMBannerAdImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<BMBannerAuctionParams>,
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)
    override val ad: Ad? get() = bannerView?.asAd()

    private var context: Context? = null
    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<BannerRequest> {
            override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
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
                            adSource = this@BMBannerAdImpl,
                        )
                    )
                )
            }

            override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                logInternal(Tag, "onRequestFailed $bmError. $this", bmError.asBidonError(demandId))
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = bmError.asBidonError(demandId).asRoundStatus(),
                )
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onRequestExpired(request: BannerRequest) {
                logInternal(Tag, "onRequestExpired: $this")
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = RoundStatus.NoBid,
                )
                adState.tryEmit(AdState.LoadFailed(DemandError.Expired(demandId)))
            }
        }
    }

    private val bannerListener by lazy {
        object : BannerListener {
            override fun onAdLoaded(bannerView: BannerView) {
                logInternal(Tag, "onAdLoaded: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adState.tryEmit(AdState.Fill(bannerView.asAd()))
            }

            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                logInternal(Tag, "onAdLoadFailed: $this", bmError.asBidonError(demandId))
                this@BMBannerAdImpl.bannerView = bannerView
                adState.tryEmit(AdState.LoadFailed(bmError.asBidonError(demandId)))
            }

            @Deprecated("Source BidMachine deprecated callback")
            override fun onAdShown(interstitialAd: BannerView) {
            }

            override fun onAdImpression(bannerView: BannerView) {
                logInternal(Tag, "onAdImpression: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adState.tryEmit(AdState.Impression(bannerView.asAd()))
            }

            override fun onAdClicked(bannerView: BannerView) {
                logInternal(Tag, "onAdClicked: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adState.tryEmit(AdState.Clicked(bannerView.asAd()))
            }

            override fun onAdExpired(bannerView: BannerView) {
                logInternal(Tag, "onAdExpired: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adState.tryEmit(AdState.Expired(bannerView.asAd()))
            }
        }
    }

    override suspend fun bid(adParams: BMBannerAuctionParams): AuctionResult {
        logInternal(Tag, "Starting with $adParams: $this")
        markBidStarted()
        context = adParams.context
        BannerRequest.Builder()
            .setSize(adParams.bannerSize.asBidMachineBannerSize())
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
            val bannerView = BannerView(context).also {
                bannerView = it
            }
            bannerView.setListener(bannerListener)
            bannerView.load(adRequest)
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

    override fun show(activity: Activity) {}

    override fun notifyLoss() {
        adRequest?.notifyMediationLoss()
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAuctionParams(
        adContainer: ViewGroup,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerSize: BannerSize,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        BMBannerAuctionParams(priceFloor = priceFloor, timeout = timeout, context = adContainer.context, bannerSize = bannerSize)
    }

    override fun destroy() {
        logInternal(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        bannerView?.destroy()
        bannerView = null
    }

    override fun getAdView(): View {
        return requireNotNull(bannerView)
    }

    private fun BannerView.asAd(): Ad {
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

    private fun BannerSize.asBidMachineBannerSize() = when (this) {
        BannerSize.Banner -> BidMachineBannerSize.Size_320x50
        BannerSize.LeaderBoard -> BidMachineBannerSize.Size_728x90
        BannerSize.MRec -> BidMachineBannerSize.Size_300x250
        BannerSize.Adaptive -> BidMachineBannerSize.Size_320x50
    }
}

private const val Tag = "BidMachine Banner"
