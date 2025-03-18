package org.bidon.chartboost.impl

import com.chartboost.sdk.ads.Banner
import com.chartboost.sdk.callbacks.BannerCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import org.bidon.chartboost.ext.asBidonLoadError
import org.bidon.chartboost.ext.asBidonShowError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class ChartboostBannerImpl :
    AdSource.Banner<ChartboostBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adView: Banner? = null

    override val isAdReadyToShow: Boolean
        get() = adView?.isCached() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ChartboostBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: ChartboostBannerAuctionParams) {
        val callback = object : BannerCallback {
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
        val adView = Banner(
            context = adParams.activity,
            location = adParams.adLocation,
            size = adParams.bannerSize,
            callback = callback,
            mediation = adParams.mediation
        ).also { adView = it }
        if (adView.isCached()) {
            logInfo(TAG, "Ad is available already")
            val ad = getAd() ?: return
            emitEvent(AdEvent.Fill(ad))
        } else {
            logInfo(TAG, "Ad is not available, caching")
            adView.cache()
        }
    }

    override fun getAdView(): AdViewHolder? = adView?.let { AdViewHolder(it) }

    override fun destroy() {
        adView?.detach()
        adView?.clearCache()
        adView = null
    }
}

private const val TAG = "ChartboostBannerImpl"