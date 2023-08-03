package org.bidon.bigoads.impl

import android.app.Activity
import android.content.Context
import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import sg.bigo.ads.BigoAdSdk
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
    AdSource.Interstitial<BigoFullscreenAuctionParams>,
    AdLoadingType.Bidding<BigoFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null && interstitialAd?.isExpired != false

    override fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoFullscreenAuctionParams(
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for Bigo Ads"
                },
                slotId = requireNotNull(json?.optString("slot_id")) {
                    "Slot id is required for Bigo Ads"
                },
                bidPrice = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for Bigo Ads"
                },
            )
        }
    }

    override fun getToken(context: Context): String? = BigoAdSdk.getBidderToken()

    override fun adRequest(adParams: BigoFullscreenAuctionParams) {
        val builder = InterstitialAdRequest.Builder()
        builder
            .withBid(adParams.payload)
            .withSlotId(adParams.slotId)
        val loader = InterstitialAdLoader.Builder().withAdLoadListener(object : AdLoadListener<InterstitialAd> {
            override fun onError(adError: AdError) {
                val error = adError.asBidonError()
                logError(TAG, "Error while loading ad: $adError. $this", error)
                emitEvent(AdEvent.LoadFailed(error))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInfo(TAG, "onAdLoaded: $interstitialAd, $this")
                this@BigoAdsInterstitialImpl.interstitialAd = interstitialAd
                interstitialAd.setAdInteractionListener(object : AdInteractionListener {
                    override fun onAdError(error: AdError) {
                        val cause = error.asBidonError()
                        logError(TAG, "onAdError: $this", cause)
                        emitEvent(AdEvent.ShowFailed(cause))
                    }

                    override fun onAdImpression() {
                        logInfo(TAG, "onAdImpression: $this")
                        val ad = getAd(this@BigoAdsInterstitialImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParams.bidPrice,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }

                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        val ad = getAd(this@BigoAdsInterstitialImpl) ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdOpened() {
                        logInfo(TAG, "onAdOpened: $this")
                        val ad = getAd(this@BigoAdsInterstitialImpl) ?: return
                        emitEvent(AdEvent.Shown(ad))
                    }

                    override fun onAdClosed() {
                        logInfo(TAG, "onAdClosed: $this")
                        val ad = getAd(this@BigoAdsInterstitialImpl) ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }
                })
                emitEvent(
                    AdEvent.Bid(
                        AuctionResult.Bidding(
                            adSource = this@BigoAdsInterstitialImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }
        })
        loader.build()
            .loadAd(builder.build())
    }

    override fun show(activity: Activity) {
        val interstitialAd = interstitialAd
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            interstitialAd.show()
        }
    }

    override fun fill() {
        val ad = getAd(this)
        if (interstitialAd != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }
}

private const val TAG = "BigoAdsInterstitial"