package org.bidon.meta.impl

import android.app.Activity
import android.content.Context
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.BidderTokenProvider
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
import org.bidon.meta.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
class MetaInterstitialImpl :
    AdSource.Interstitial<MetaFullscreenAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MetaFullscreenAuctionParams? = null
    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isAdLoaded ?: false

    override suspend fun getToken(context: Context): String? {
        return BidderTokenProvider.getBidderToken(context)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MetaFullscreenAuctionParams(
                context = activity.applicationContext,
                placementId = requireNotNull(json?.optString("placement_id")) {
                    "Placement id is required for Meta"
                },
                price = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for Meta"
                },
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for Meta"
                },
            )
        }
    }

    override fun load(adParams: MetaFullscreenAuctionParams) {
        this.adParams = adParams
        val interstitial = InterstitialAd(adParams.context, adParams.placementId).also {
            interstitialAd = it
        }
        interstitial.loadAd(
            interstitial.buildLoadAdConfig()
                .withAdListener(object : InterstitialAdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        val error = adError.asBidonError()
                        logError(TAG, "Error while loading ad(${adError?.errorCode}: ${adError?.errorMessage}). $this", error)
                        emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        logInfo(TAG, "onAdLoaded $ad: $interstitialAd, $this")
                        val bidonAd = getAd(this)
                        if (interstitialAd != null && bidonAd != null) {
                            emitEvent(AdEvent.Fill(bidonAd))
                        } else {
                            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                        }
                    }

                    override fun onAdClicked(ad: Ad?) {
                        logInfo(TAG, "onAdClicked: $this")
                        val bidonAd = getAd(this@MetaInterstitialImpl) ?: return
                        emitEvent(AdEvent.Clicked(bidonAd))
                    }

                    override fun onLoggingImpression(ad: Ad?) {
                        logInfo(TAG, "onAdImpression: $this")
                        val bidonAd = getAd(this@MetaInterstitialImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = bidonAd,
                                adValue = AdValue(
                                    adRevenue = adParams.price,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }

                    override fun onInterstitialDisplayed(ad: Ad?) {
                        logInfo(TAG, "onInterstitialDisplayed $ad: $this")
                        val bidonAd = getAd(this@MetaInterstitialImpl) ?: return
                        emitEvent(AdEvent.Shown(bidonAd))
                    }

                    override fun onInterstitialDismissed(ad: Ad?) {
                        logInfo(TAG, "onInterstitialDismissed $ad: $this")
                        val bidonAd = getAd(this@MetaInterstitialImpl) ?: return
                        emitEvent(AdEvent.Closed(bidonAd))
                    }
                })
                .withBid(adParams.payload)
                .build()
        )
    }

    override fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
        adParams = null
    }

    override fun show(activity: Activity) {
        val interstitialAd = interstitialAd
        if (interstitialAd != null && interstitialAd.isAdLoaded) {
            interstitialAd.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "MetaInterstitialImpl"