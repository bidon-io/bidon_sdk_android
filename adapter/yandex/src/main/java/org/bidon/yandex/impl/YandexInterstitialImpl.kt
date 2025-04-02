package org.bidon.yandex.impl

import android.app.Activity
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.yandex.ext.asBidonAdValue
import org.bidon.yandex.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 17/09/2023.
 */
internal class YandexInterstitialImpl :
    AdSource.Interstitial<YandexFullscreenAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl(),
    YandexLoader by singleLoader {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            YandexFullscreenAuctionParam(
                context = activity.applicationContext,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: YandexFullscreenAuctionParam) {
        val adUnitId = adParams.adUnitId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")))

        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        val adLoadListener = object : InterstitialAdLoadListener {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@YandexInterstitialImpl.interstitialAd = interstitialAd
                logInfo(TAG, "onAdLoaded: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Fill(ad))
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                logInfo(TAG, "onAdFailedToLoad: ${error.code} ${error.description}. $this")
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }
        }
        requestInterstitialAd(adParams.context, adRequestConfiguration, adLoadListener)
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            interstitialAd?.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    logInfo(TAG, "onAdShown: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Shown(ad))
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onAdDismissed() {
                    logInfo(TAG, "onAdDismissed: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Closed(ad))
                }

                override fun onAdFailedToShow(adError: AdError) {
                    logInfo(TAG, "onAdFailedToShow: ${adError.description}. $this")
                    emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                }

                override fun onAdImpression(impressionData: ImpressionData?) {
                    logInfo(TAG, "onAdImpression: $this")
                    val ad = getAd() ?: return
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = impressionData.asBidonAdValue()
                        )
                    )
                }
            })
            interstitialAd?.show(activity)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
    }
}

private const val TAG = "YandexInterstitialImpl"