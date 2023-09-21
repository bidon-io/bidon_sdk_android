package org.bidon.yandex.impl

import android.app.Activity
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
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
import org.bidon.yandex.YandexDemandId

/**
 * Created by Aleksei Cherniaev on 17/09/2023.
 */
internal class YandexInterstitialImpl :
    AdSource.Interstitial<YandexFullscreenAuctionParam>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null
    private var param: YandexFullscreenAuctionParam? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isLoaded == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            YandexFullscreenAuctionParam(
                lineItem = popLineItem(YandexDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                context = activity.applicationContext,
            )
        }
    }

    override fun load(adParams: YandexFullscreenAuctionParam) {
        logInfo(TAG, "Starting with $adParams")
        this.param = adParams
        val interstitialAd = InterstitialAd(adParams.context)
        interstitialAd.setAdUnitId(adParams.adUnitId)
        interstitialAd.setInterstitialAdEventListener(
            object : InterstitialAdEventListener {
                override fun onAdLoaded() {
                    this@YandexInterstitialImpl.interstitialAd = interstitialAd
                    val ad = getAd(interstitialAd) ?: return
                    emitEvent(AdEvent.Fill(ad))
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    logError(TAG, "onAdFailedToLoad: ${error.code} ${error.description}. $this", BidonError.NoFill(demandId))
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdShown() {
                    logInfo(TAG, "onAdShown: $this")
                    val ad = getAd(this@YandexInterstitialImpl) ?: return
                    emitEvent(AdEvent.Shown(ad))
                }

                override fun onAdDismissed() {
                    logInfo(TAG, "onAdDismissed: $this")
                    val ad = getAd(this@YandexInterstitialImpl) ?: return
                    emitEvent(AdEvent.Closed(ad))
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    val ad = getAd(this@YandexInterstitialImpl) ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}

                override fun onImpression(impressionData: ImpressionData?) {
                    logInfo(TAG, "onImpression: $this")
                    val ad = getAd(this@YandexInterstitialImpl) ?: return
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = adParams.price / 1000.0,
                                currency = USD,
                                precision = Precision.Estimated
                            )
                        )
                    )
                }
            }
        )
        interstitialAd.loadAd(AdRequest.Builder().build())
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            interstitialAd?.show()
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy: $this")
        interstitialAd?.destroy()
        interstitialAd = null
        param = null
    }
}

private const val TAG = "YandexInterstitialImpl"