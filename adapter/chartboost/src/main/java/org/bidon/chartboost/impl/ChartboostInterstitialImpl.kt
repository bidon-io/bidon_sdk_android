package org.bidon.chartboost.impl

import android.app.Activity
import com.chartboost.sdk.ads.Interstitial
import com.chartboost.sdk.callbacks.InterstitialCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.DismissEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import org.bidon.chartboost.ext.asBidonLoadError
import org.bidon.chartboost.ext.asBidonShowError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class ChartboostInterstitialImpl :
    AdSource.Interstitial<ChartboostFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: Interstitial? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isCached() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ChartboostFullscreenAuctionParams(adUnit = adUnit)
        }
    }

    override fun load(adParams: ChartboostFullscreenAuctionParams) {
        val callback = object : InterstitialCallback {
            override fun onAdLoaded(event: CacheEvent, error: CacheError?) {
                if (error == null) {
                    logInfo(TAG, "onAdLoaded $event")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Fill(ad))
                } else {
                    logInfo(TAG, "onAdFailed $event $error")
                    emitEvent(AdEvent.LoadFailed(error.asBidonLoadError()))
                }
            }

            override fun onAdRequestedToShow(event: ShowEvent) {
                logInfo(TAG, "onAdRequestedToShow $event")
            }

            override fun onAdShown(event: ShowEvent, error: ShowError?) {
                logInfo(TAG, "onAdShown $event")
                if (error == null) {
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Shown(ad))
                } else {
                    emitEvent(AdEvent.ShowFailed(error.asBidonShowError()))
                }
            }

            override fun onAdClicked(event: ClickEvent, error: ClickError?) {
                logInfo(TAG, "onAdClicked $event")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdDismiss(event: DismissEvent) {
                logInfo(TAG, "onImpressionRecorded $event")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Closed(ad))
            }

            override fun onImpressionRecorded(event: ImpressionEvent) {
                logInfo(TAG, "onImpressionRecorded $event")
                val ad = getAd() ?: return
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = adParams.price / 1000.0,
                            currency = AdValue.USD,
                            Precision.Precise
                        )
                    )
                )
            }
        }

        val interstitialAd = Interstitial(
            location = adParams.adLocation,
            callback = callback,
            mediation = adParams.mediation
        ).also { interstitialAd = it }
        if (interstitialAd.isCached()) {
            logInfo(TAG, "Ad is available already")
            val ad = getAd() ?: return
            emitEvent(AdEvent.Fill(ad))
        } else {
            logInfo(TAG, "Ad is not available, caching")
            interstitialAd.cache()
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            interstitialAd?.show()
        } else {
            logInfo(TAG, "Ad is not ready to show")
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        interstitialAd?.clearCache()
        interstitialAd = null
    }
}

private const val TAG = "ChartboostInterstitialImpl"