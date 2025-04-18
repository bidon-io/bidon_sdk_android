package org.bidon.amazon.impl

import android.app.Activity
import android.view.View
import com.amazon.device.ads.DTBActivityMonitor
import com.amazon.device.ads.DTBAdInterstitial
import com.amazon.device.ads.DTBAdInterstitialListener
import com.amazon.device.ads.SDKUtilities
import org.bidon.amazon.AmazonBidManager
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AmazonInterstitialImpl(private val bidManager: AmazonBidManager) :
    AdSource.Interstitial<FullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitial: DTBAdInterstitial? = null

    override val isAdReadyToShow: Boolean
        get() = interstitial != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            FullscreenAuctionParams(
                activity = activity,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: FullscreenAuctionParams) {
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
        val interstitialAd = DTBAdInterstitial(
            adParams.activity,
            object : DTBAdInterstitialListener {
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

                override fun onAdClosed(view: View?) {
                    logInfo(TAG, "onAdClosed")
                    emitEvent(AdEvent.Closed(getAd() ?: return))
                    interstitial = null
                }

                override fun onImpressionFired(view: View?) {
                    logInfo(TAG, "onImpressionFired")
                    getAd()?.let {
                        emitEvent(AdEvent.Shown(it))
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = it,
                                adValue = AdValue(
                                    adRevenue = adParams.price / 1000.0,
                                    currency = AdValue.USD,
                                    Precision.Precise
                                )
                            )
                        )
                    }
                }

                override fun onVideoCompleted(view: View?) {
                    super.onVideoCompleted(view)
                    logInfo(TAG, "onVideoCompleted")
                }
            }
        ).also {
            interstitial = it
        }
        val bidInfo = SDKUtilities.getBidInfo(dtbAdResponse)
        interstitialAd.fetchAd(bidInfo)
    }

    override fun show(activity: Activity) {
        val interstitial = interstitial
        if (interstitial == null) {
            logError(TAG, "Interstitial is null", BidonError.AdNotReady)
            emitEvent(AdEvent.LoadFailed(BidonError.AdNotReady))
            return
        }
        interstitial.show()
    }

    override fun destroy() {
        interstitial = null
    }
}

private const val TAG = "AmazonInterstitialImpl"