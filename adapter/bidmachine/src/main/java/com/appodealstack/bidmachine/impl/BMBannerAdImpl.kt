package com.appodealstack.bidmachine.impl

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.appodealstack.bidmachine.BMAuctionResult
import com.appodealstack.bidmachine.BMBannerAuctionParams
import com.appodealstack.bidmachine.BidMachineBannerSize
import com.appodealstack.bidmachine.asBidonError
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.adapter.DemandId
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.ads.banner.helper.impl.dpToPx
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

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
    override val ad: Ad? get() = bannerView?.asAd()

    private var context: Context? = null
    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null
    private var bannerFormat: BannerFormat? = null

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<BannerRequest> {
            override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
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
                            adSource = this@BMBannerAdImpl,
                        )
                    )
                )
            }

            override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                logError(Tag, "onRequestFailed $bmError. $this", bmError.asBidonError(demandId))
                adRequest = request
                markBidFinished(
                    ecpm = null,
                    roundStatus = bmError.asBidonError(demandId).asRoundStatus(),
                )
                adEvent.tryEmit(AdEvent.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onRequestExpired(request: BannerRequest) {
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

    private val bannerListener by lazy {
        object : BannerListener {
            override fun onAdLoaded(bannerView: BannerView) {
                logInfo(Tag, "onAdLoaded: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.Fill(bannerView.asAd()))
            }

            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                logError(Tag, "onAdLoadFailed: $this", bmError.asBidonError(demandId))
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.LoadFailed(bmError.asBidonError(demandId)))
            }

            override fun onAdImpression(bannerView: BannerView) {
                logInfo(Tag, "onAdShown: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.Shown(bannerView.asAd()))
                adEvent.tryEmit(AdEvent.PaidRevenue(bannerView.asAd()))
            }

            override fun onAdClicked(bannerView: BannerView) {
                logInfo(Tag, "onAdClicked: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.Clicked(bannerView.asAd()))
            }

            override fun onAdExpired(bannerView: BannerView) {
                logInfo(Tag, "onAdExpired: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.Expired(bannerView.asAd()))
            }
        }
    }

    override suspend fun bid(adParams: BMBannerAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted()
        context = adParams.context
        bannerFormat = adParams.bannerFormat
        BannerRequest.Builder()
            .setSize(adParams.bannerFormat.asBidMachineBannerSize())
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
            val bannerView = BannerView(context).also {
                bannerView = it
            }
            bannerView.setListener(bannerListener)
            bannerView.load(adRequest)
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
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        BMBannerAuctionParams(priceFloor = priceFloor, timeout = timeout, context = adContainer.context, bannerFormat = bannerFormat)
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        bannerView?.destroy()
        bannerView = null
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(bannerView)
        return AdViewHolder(
            networkAdview = adView,
            widthPx = FrameLayout.LayoutParams.MATCH_PARENT,
            heightPx = when (bannerFormat) {
                BannerFormat.Adaptive,
                BannerFormat.Banner -> 50.dpToPx
                BannerFormat.LeaderBoard -> 90.dpToPx
                BannerFormat.MRec -> 250.dpToPx
                null -> FrameLayout.LayoutParams.WRAP_CONTENT
            }
        )
    }

    private fun BannerView.asAd(): Ad {
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

    private fun BannerFormat.asBidMachineBannerSize() = when (this) {
        BannerFormat.Banner -> BidMachineBannerSize.Size_320x50
        BannerFormat.LeaderBoard -> BidMachineBannerSize.Size_728x90
        BannerFormat.MRec -> BidMachineBannerSize.Size_300x250
        BannerFormat.Adaptive -> BidMachineBannerSize.Size_320x50
    }
}

private const val Tag = "BidMachine Banner"
