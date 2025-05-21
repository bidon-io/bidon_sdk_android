package org.bidon.bidmachine.impl

import io.bidmachine.AdRequest
import io.bidmachine.PriceFloorParams
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError
import org.bidon.bidmachine.BMAuctionResult
import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.asBidonErrorOnBid
import org.bidon.bidmachine.asBidonErrorOnFill
import org.bidon.bidmachine.ext.asBidMachineBannerSize
import org.bidon.bidmachine.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class BMBannerAdImpl(
    private val obtainAdAuctionParams: GetAdAuctionParamUseCase = GetAdAuctionParamUseCase()
) :
    AdSource.Banner<BMBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    WinLossNotifiable,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adRequest: BannerRequest? = null
    private var bannerView: BannerView? = null

    override val isAdReadyToShow: Boolean
        get() = bannerView?.canShow() == true

    override fun load(adParams: BMBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.activity.runOnUiThread {
            bannerView = BannerView(adParams.activity.applicationContext)
            val bidType = adParams.adUnit.bidType
            val requestBuilder = BannerRequest.Builder()
                .apply {
                    if (bidType == BidType.CPM) {
                        this.setNetworks("")
                    }
                }
                .setSize(adParams.bannerFormat.asBidMachineBannerSize())
                .setPriceFloorParams(PriceFloorParams().addPriceFloor(adParams.price))
                .setCustomParams(adParams.customParameters)
                .setLoadingTimeOut(adParams.timeout.toInt())
                .setListener(
                    object : AdRequest.AdRequestListener<BannerRequest> {
                        override fun onRequestSuccess(request: BannerRequest, result: BMAuctionResult) {
                            logInfo(TAG, "onRequestSuccess $result: $this")
                            fillRequest(request, bidType)
                        }

                        override fun onRequestFailed(request: BannerRequest, bmError: BMError) {
                            val error = if (bidType == BidType.RTB) {
                                bmError.asBidonErrorOnBid(demandId)
                            } else {
                                bmError.asBidonErrorOnFill(demandId)
                            }
                            logError(TAG, "onRequestFailed $bmError. $this", error)
                            emitEvent(AdEvent.LoadFailed(error))
                        }

                        override fun onRequestExpired(request: BannerRequest) {
                            logInfo(TAG, "onRequestExpired: $this")
                            emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
                        }
                    }
                )
            if (bidType == BidType.RTB) {
                adParams.payload?.let {
                    requestBuilder.setBidPayload(it)
                } ?: run {
                    emitEvent(
                        AdEvent.LoadFailed(
                            BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                        )
                    )
                    return@runOnUiThread
                }
            }
            requestBuilder.build()
                .also { adRequest = it }
                .request(adParams.activity.applicationContext)
        }
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams.getBMBannerAuctionParams(auctionParamsScope)
    }

    override fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double) {
        logInfo(TAG, "notifyLoss: $this")
        adRequest?.notifyMediationLoss(winnerNetworkName, winnerNetworkPrice)
    }

    override fun notifyWin() {
        logInfo(TAG, "notifyWin: $this")
        adRequest?.notifyMediationWin()
    }

    override fun getAdView(): AdViewHolder? = bannerView?.let { AdViewHolder(it) }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adRequest?.destroy()
        adRequest = null
        bannerView?.setListener(null)
        bannerView?.destroy()
        bannerView = null
    }

    private fun fillRequest(adRequest: BannerRequest?, bidType: BidType) {
        logInfo(TAG, "Starting fill: $this")
        val bannerView = bannerView
        if (bannerView == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.NoContextFound))
        } else {
            bannerView.setListener(
                object : BannerListener {

                    override fun onAdLoaded(bannerView: BannerView) {
                        logInfo(TAG, "onAdLoaded: $this")
                        setDsp(bannerView.auctionResult?.demandSource)
                        if (bidType == BidType.CPM) {
                            setPrice(bannerView.auctionResult?.price ?: 0.0)
                        }
                        getAd()?.let {
                            emitEvent(AdEvent.Fill(it))
                        }
                    }

                    override fun onAdShowFailed(bannerView: BannerView, bmError: BMError) {
                        logInfo(TAG, "onAdShowFailed: $this")
                        emitEvent(AdEvent.ShowFailed(bmError.asBidonErrorOnFill(demandId)))
                    }

                    override fun onAdLoadFailed(bannerView: BannerView, bmError: BMError) {
                        logInfo(TAG, "onRequestFailed $bmError. $this")
                        emitEvent(AdEvent.LoadFailed(bmError.asBidonErrorOnFill(demandId)))
                    }

                    override fun onAdImpression(bannerView: BannerView) {
                        logInfo(TAG, "onAdShown: $this")
                        // tracked impression/shown by [BannerView]
                        getAd()?.let {
                            emitEvent(
                                AdEvent.PaidRevenue(
                                    ad = it,
                                    adValue = bannerView.auctionResult.asBidonAdValue()
                                )
                            )
                        }
                    }

                    override fun onAdClicked(bannerView: BannerView) {
                        logInfo(TAG, "onAdClicked: $this")
                        getAd()?.let {
                            emitEvent(AdEvent.Clicked(it))
                        }
                    }

                    override fun onAdExpired(bannerView: BannerView) {
                        logInfo(TAG, "onAdExpired: $this")
                        getAd()?.let {
                            emitEvent(AdEvent.Expired(it))
                        }
                    }
                }
            )
            bannerView.load(adRequest)
        }
    }
}

private const val TAG = "BidMachineBanner"
