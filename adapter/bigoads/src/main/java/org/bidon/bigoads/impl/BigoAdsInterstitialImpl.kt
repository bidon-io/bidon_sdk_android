package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.bigoads.ext.asBidonError
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
import org.bidon.sdk.stats.models.BidType
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.InterstitialAd
import sg.bigo.ads.api.InterstitialAdLoader
import sg.bigo.ads.api.InterstitialAdRequest

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal class BigoAdsInterstitialImpl :
    AdSource.Interstitial<BigoAdsFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null && interstitialAd?.isExpired != false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoAdsFullscreenAuctionParams(adUnit = adUnit)
        }
    }

    override fun load(adParams: BigoAdsFullscreenAuctionParams) {
        val slotId = adParams.slotId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "slotId")))

        val loader = InterstitialAdLoader.Builder()
            .withAdLoadListener(object : AdLoadListener<InterstitialAd> {
                override fun onError(adError: AdError) {
                    val error = adError.asBidonError()
                    logError(TAG, "Error while loading ad: $adError. $this", error)
                    emitEvent(AdEvent.LoadFailed(error))
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdLoaded: $interstitialAd, $this")
                    this@BigoAdsInterstitialImpl.interstitialAd = interstitialAd
                    fill(interstitialAd, adParams)
                }
            })
            .build()

        val adRequestBuilder = InterstitialAdRequest.Builder()
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            adRequestBuilder.withBid(payload)
        }
        adRequestBuilder.withSlotId(slotId)
        loader.loadAd(adRequestBuilder.build())
    }

    override fun show(activity: Activity) {
        val interstitialAd = interstitialAd
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            interstitialAd.show()
        }
    }

    override fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
    }

    private fun fill(
        interstitialAd: InterstitialAd,
        adParams: BigoAdsFullscreenAuctionParams
    ) {
        interstitialAd.setAdInteractionListener(object : AdInteractionListener {
            override fun onAdError(error: AdError) {
                val cause = error.asBidonError()
                logError(TAG, "onAdError: $this", cause)
                emitEvent(AdEvent.ShowFailed(cause))
            }

            override fun onAdImpression() {
                logInfo(TAG, "onAdImpression: $this")
                getAd()?.let { ad ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = adParams.price / 1000.0,
                                precision = Precision.Precise,
                                currency = AdValue.USD,
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Clicked(ad))
                }
            }

            override fun onAdOpened() {
                logInfo(TAG, "onAdOpened: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Shown(ad))
                }
            }

            override fun onAdClosed() {
                logInfo(TAG, "onAdClosed: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Closed(ad))
                }
                this@BigoAdsInterstitialImpl.interstitialAd = null
            }
        })
        getAd()?.let { ad ->
            emitEvent(AdEvent.Fill(ad))
        }
    }
}

private const val TAG = "BigoAdsInterstitial"