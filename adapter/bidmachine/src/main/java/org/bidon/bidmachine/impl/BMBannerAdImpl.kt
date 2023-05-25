package org.bidon.bidmachine.impl

import android.app.Activity
import android.content.Context
import io.bidmachine.AdRequest
import io.bidmachine.CustomParams
import io.bidmachine.PriceFloorParams
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.BidMachineBannerSize
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

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
        demandId = demandId,
        demandAd = demandAd,
    ) {

    override val adEvent =
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val ad: Ad? get() = bannerView?.asAd()

    private var context: Context? = null
    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null
    private var bannerFormat: BannerFormat? = null
    override val isAdReadyToShow: Boolean
        get() = bannerView?.canShow() == true

    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<BannerRequest> {
            override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
                logInfo(Tag, "onRequestSuccess $result: $this")
                adRequest = request
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = result.price,
                            adSource = this@BMBannerAdImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                val error = bmError.asBidonErrorOnBid(demandId)
                logError(Tag, "onRequestFailed $bmError. $this", error)
                adRequest = request
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onRequestExpired(request: BannerRequest) {
                logInfo(Tag, "onRequestExpired: $this")
                adRequest = request
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
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(Tag, "onAdLoadFailed: $this", error)
                this@BMBannerAdImpl.bannerView = bannerView
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }

            override fun onAdImpression(bannerView: BannerView) {
                logInfo(Tag, "onAdShown: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                // tracked impression/shown by [BannerView]
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = bannerView.asAd(),
                        adValue = bannerView.auctionResult.asBidonAdValue()
                    )
                )
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

    override fun bid(adParams: BMBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        context = adParams.context
        bannerFormat = adParams.bannerFormat
        BannerRequest.Builder()
            .setSize(adParams.bannerFormat.asBidMachineBannerSize())
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
            val bannerView = BannerView(context).also {
                bannerView = it
            }
            bannerView.setListener(bannerListener)
            bannerView.load(adRequest)
        }
    }

    override fun show(activity: Activity) {}

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
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit,
        containerWidth: Float
    ): Result<AdAuctionParams> = runCatching {
        BMBannerAuctionParams(
            pricefloor = pricefloor,
            timeout = timeout,
            context = activity.applicationContext,
            bannerFormat = bannerFormat
        )
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        bannerView?.setListener(null)
        bannerView?.destroy()
        bannerView = null
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(bannerView)
        return AdViewHolder(
            networkAdview = adView,
            widthDp = bannerFormat?.asBidMachineBannerSize()?.width ?: bannerFormat.getWidthDp(),
            heightDp = bannerFormat?.asBidMachineBannerSize()?.height ?: bannerFormat.getHeightDp()
        )
    }

    private fun BannerView.asAd(): Ad {
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

    private fun BannerFormat.asBidMachineBannerSize() = when (this) {
        BannerFormat.Banner -> BidMachineBannerSize.Size_320x50
        BannerFormat.LeaderBoard -> BidMachineBannerSize.Size_728x90
        BannerFormat.MRec -> BidMachineBannerSize.Size_300x250
        BannerFormat.Adaptive -> if (isTablet) {
            BidMachineBannerSize.Size_728x90
        } else {
            BidMachineBannerSize.Size_320x50
        }
    }
}

private const val Tag = "BidMachine Banner"
