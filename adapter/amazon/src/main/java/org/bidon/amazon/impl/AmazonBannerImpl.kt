package org.bidon.amazon.impl

import android.view.View
import com.amazon.device.ads.DTBActivityMonitor
import com.amazon.device.ads.DTBAdBannerListener
import com.amazon.device.ads.DTBAdView
import com.amazon.device.ads.SDKUtilities
import org.bidon.amazon.AmazonBidManager
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AmazonBannerImpl(private val bidManager: AmazonBidManager) :
    AdSource.Banner<BannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adView: DTBAdView? = null

    override val isAdReadyToShow: Boolean
        get() = adView != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: BannerAuctionParams) {
        val slotUuid = adParams.slotUuid
        if (slotUuid == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, "slotUuid")))
            return
        }

        val dtbAdResponse = bidManager.getResponse(slotUuid)
        if (dtbAdResponse == null) {
            logError(TAG, "DTBAdResponse is null", BidonError.NoBid)
            emitEvent(AdEvent.LoadFailed(BidonError.NoBid))
            return
        }
        DTBActivityMonitor.setActivity(adParams.activity)
        val adView = DTBAdView(
            adParams.activity.applicationContext,
            object : DTBAdBannerListener {
                override fun onAdLoaded(view: View?) {
                    logInfo(TAG, "onAdLoaded")
                    emitEvent(AdEvent.Fill(getAd() ?: return))
                }

                override fun onAdFailed(view: View?) {
                    logInfo(TAG, "onAdFailed")
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdClicked(view: View?) {
                    logInfo(TAG, "onAdClicked")
                    emitEvent(AdEvent.Clicked(getAd() ?: return))
                }

                override fun onAdLeftApplication(view: View?) {}
                override fun onAdOpen(view: View?) {}

                override fun onAdClosed(view: View?) {}

                override fun onImpressionFired(view: View?) {
                    logInfo(TAG, "onImpressionFired")
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = getAd() ?: return,
                            adValue = AdValue(
                                adRevenue = adParams.price / 1000.0,
                                currency = USD,
                                Precision.Precise
                            )
                        )
                    )
                }
            }
        ).also {
            this.adView = it
        }
        val bidInfo = SDKUtilities.getBidInfo(dtbAdResponse)
        adView.fetchAd(bidInfo)
    }

    override fun getAdView(): AdViewHolder? = adView?.let { AdViewHolder(it) }

    override fun destroy() {
        adView?.destroy()
        adView = null
    }
}

private const val TAG = "AmazonBannerImpl"
