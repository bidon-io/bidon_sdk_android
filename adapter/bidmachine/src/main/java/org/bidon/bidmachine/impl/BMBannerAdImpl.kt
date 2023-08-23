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
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class BMBannerAdImpl :
    AdSource.Banner<BMBannerAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var context: Context? = null
    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null
    private var bannerFormat: BannerFormat? = null

    override val isAdReadyToShow: Boolean
        get() = bannerView?.canShow() == true

    override suspend fun getToken(context: Context): String {
        return BidMachine.getBidToken(context)
    }

    override fun load(adParams: BMBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        context = adParams.context
        bannerFormat = adParams.bannerFormat
        val requestBuilder = BannerRequest.Builder()
            .setSize(adParams.bannerFormat.asBidMachineBannerSize())
            .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
            .setCustomParams(CustomParams().addParam("mediation_mode", "bidon"))
            .setLoadingTimeOut(adParams.timeout.toInt())
            .setListener(
                object : AdRequest.AdRequestListener<BannerRequest> {
                    override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
                        logInfo(TAG, "onRequestSuccess $result: $this")
                        fillRequest(request)
                    }

                    override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                        val error = bmError.asBidonErrorOnBid(demandId)
                        logError(TAG, "onRequestFailed $bmError. $this", error)
                        emitEvent(AdEvent.LoadFailed(error))
                    }

                    override fun onRequestExpired(request: BannerRequest) {
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

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BMBannerAuctionParams(
                price = pricefloor,
                timeout = timeout,
                context = activity.applicationContext,
                bannerFormat = bannerFormat,
                payload = json?.optString("payload")
            )
        }
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        adRequest?.notifyMediationWin()
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(bannerView)
        return AdViewHolder(
            networkAdview = adView,
            widthDp = bannerFormat?.asBidMachineBannerSize()?.width ?: bannerFormat.getWidthDp(),
            heightDp = bannerFormat?.asBidMachineBannerSize()?.height ?: bannerFormat.getHeightDp()
        )
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        bannerView?.setListener(null)
        bannerView?.destroy()
        bannerView = null
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
            bannerView.setListener(
                object : BannerListener {

                    override fun onAdLoaded(bannerView: BannerView) {
                        logInfo(TAG, "onAdLoaded: $this")
                        emitEvent(AdEvent.Fill(bannerView.asAd()))
                    }

                    override fun onAdShowFailed(bannerView: BannerView, bmError: BMError) {
                        logInfo(TAG, "onAdShowFailed: $this")
                        emitEvent(AdEvent.ShowFailed(bmError.asBidonErrorOnFill(demandId)))
                    }

                    override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                        val error = bmError.asBidonErrorOnFill(demandId)
                        logError(TAG, "onAdLoadFailed: $this", error)
                        emitEvent(AdEvent.LoadFailed(error))
                    }

                    override fun onAdImpression(bannerView: BannerView) {
                        logInfo(TAG, "onAdShown: $this")
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
                        emitEvent(AdEvent.Clicked(bannerView.asAd()))
                    }

                    override fun onAdExpired(bannerView: BannerView) {
                        logInfo(TAG, "onAdExpired: $this")
                        emitEvent(AdEvent.Expired(bannerView.asAd()))
                    }
                }
            )
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
