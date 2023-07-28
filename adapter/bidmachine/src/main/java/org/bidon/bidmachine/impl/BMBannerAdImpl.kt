package org.bidon.bidmachine.impl

import android.content.Context
import io.bidmachine.AdRequest
import io.bidmachine.BidMachine
import io.bidmachine.CustomParams
import io.bidmachine.PriceFloorParams
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.BidMachineBannerSize
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

internal class BMBannerAdImpl :
    AdSource.Banner<BMBannerAuctionParams>,
    AdLoadingType.Bidding<BMBannerAuctionParams>,
    AdLoadingType.Network<BMBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null
    private var bannerFormat: BannerFormat? = null
    override val isAdReadyToShow: Boolean
        get() = bannerView?.canShow() == true

    private var isBiddingRequest = true
    private val requestListener by lazy {
        object : AdRequest.AdRequestListener<BannerRequest> {
            override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
                logInfo(TAG, "onRequestSuccess $result: $this")
                adRequest = request
                when (isBiddingRequest) {
                    false -> {
                        fillRequest(request)
                    }

                    true -> {
                        emitEvent(
                            AdEvent.Bid(
                                AuctionResult.Bidding(
                                    adSource = this@BMBannerAdImpl,
                                    roundStatus = RoundStatus.Successful
                                )
                            )
                        )
                    }
                }
            }

            override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                val error = bmError.asBidonErrorOnBid(demandId)
                logError(TAG, "onRequestFailed $bmError. $this", error)
                adRequest = request
                emitEvent(AdEvent.LoadFailed(error))
            }

            override fun onRequestExpired(request: BannerRequest) {
                logInfo(TAG, "onRequestExpired: $this")
                adRequest = request
                emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }
        }
    }

    private val bannerListener by lazy {
        object : BannerListener {
            override fun onAdLoaded(bannerView: BannerView) {
                logInfo(TAG, "onAdLoaded: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                emitEvent(AdEvent.Fill(bannerView.asAd()))
            }

            override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                val error = bmError.asBidonErrorOnFill(demandId)
                logError(TAG, "onAdLoadFailed: $this", error)
                this@BMBannerAdImpl.bannerView = bannerView
                emitEvent(AdEvent.LoadFailed(error))
            }

            override fun onAdImpression(bannerView: BannerView) {
                logInfo(TAG, "onAdShown: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                // tracked impression/shown by [BannerView]
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = bannerView.asAd(),
                        adValue = bannerView.auctionResult.asBidonAdValue()
                    )
                )
            }

            override fun onAdClicked(bannerView: BannerView) {
                logInfo(TAG, "onAdClicked: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                emitEvent(AdEvent.Clicked(bannerView.asAd()))
            }

            override fun onAdExpired(bannerView: BannerView) {
                logInfo(TAG, "onAdExpired: $this")
                this@BMBannerAdImpl.bannerView = bannerView
                emitEvent(AdEvent.Expired(bannerView.asAd()))
            }
        }
    }

    override fun getToken(context: Context): String = BidMachine.getBidToken(context)

    override fun adRequest(adParams: BMBannerAuctionParams) {
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
    override fun fill(adParams: BMBannerAuctionParams) {
        isBiddingRequest = false
        request(adParams, requestListener)
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMBannerAuctionParams(
                pricefloor = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                bannerFormat = bannerFormat,
                payload = payload
            )
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
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

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    private fun request(adParams: BMBannerAuctionParams, requestListener: AdRequest.AdRequestListener<BannerRequest>) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        bannerFormat = adParams.bannerFormat
        val requestBuilder = BannerRequest.Builder()
            .setSize(adParams.bannerFormat.asBidMachineBannerSize())
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.pricefloor))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
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

    private fun fillRequest(adRequest: BannerRequest?) {
        logInfo(TAG, "Starting fill: $this")
        val context = context
        if (context == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            val bannerView = BannerView(context).also {
                bannerView = it
            }
            bannerView.setListener(bannerListener)
            bannerView.load(adRequest)
        }
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

private const val TAG = "BidMachineBanner"
