package org.bidon.bigoads.impl

import android.app.Activity
import android.content.Context
import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
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
    AdEventFlow by AdEventFlowImpl(),
    Mode.Bidding,
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null && interstitialAd?.isExpired != false

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? = BigoAdSdk.getBidderToken()

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoFullscreenAuctionParams(
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for Bigo Ads"
                },
                slotId = requireNotNull(json?.optString("slot_id")) {
                    "Slot id is required for Bigo Ads"
                },
                bidPrice = pricefloor,
            )
        }
    }

    override fun load(adParams: BigoFullscreenAuctionParams) {
        val builder = InterstitialAdRequest.Builder()
        builder
            .withBid(adParams.payload)
            .withSlotId(adParams.slotId)
        val loader = InterstitialAdLoader.Builder()
            .withAdLoadListener(object : AdLoadListener<InterstitialAd> {
                override fun onError(adError: AdError) {
                    val error = adError.asBidonError()
                    logError(TAG, "Error while loading ad: $adError. $this", error)
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    logInfo(TAG, "onAdLoaded: $interstitialAd, $this")
                    this@BigoAdsInterstitialImpl.interstitialAd = interstitialAd
                    fill(interstitialAd, adParams)
                }
            })
        loader.build()
            .loadAd(builder.build())
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
        adParams: BigoFullscreenAuctionParams
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
                                adRevenue = adParams.bidPrice / 1000.0,
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