package org.bidon.vungle.impl

import android.app.Activity
import android.content.Context
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.BaseAdListener
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
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
import org.bidon.vungle.VungleFullscreenAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleInterstitialImpl :
    AdSource.Interstitial<VungleFullscreenAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.canPlayAd() == true

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? =
        VungleAds.getBiddingToken(context)

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleFullscreenAuctionParams(
                activity = auctionParamsScope.activity,
                placementId = requireNotNull(json?.getString("placement_id")) {
                    "Bid price is required for Bigo Ads"
                },
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required for Bigo Ads"
                }
            )
        }
    }

    override fun load(adParams: VungleFullscreenAuctionParams) {
        val interstitialAd = InterstitialAd(adParams.activity, adParams.placementId, AdConfig()).also {
            this.interstitialAd = it
        }
        interstitialAd.adListener = object : BaseAdListener {
            override fun onAdLoaded(baseAd: BaseAd) {
                val ad = getAd()
                if (ad != null) {
                    emitEvent(AdEvent.Fill(ad))
                } else {
                    emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                }
            }

            override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onError placementId=${baseAd.placementId}. $this", null)
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdImpression(baseAd: BaseAd) {
                logInfo(TAG, "onAdViewed: $this")
                val ad = getAd() ?: return
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

            override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onAdError: $this", adError)
                emitEvent(AdEvent.ShowFailed(adError.asBidonError()))
            }

            override fun onAdStart(baseAd: BaseAd) {
                logInfo(TAG, "onAdStart: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Shown(ad))
            }

            override fun onAdClicked(baseAd: BaseAd) {
                logInfo(TAG, "onAdClick: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdEnd(baseAd: BaseAd) {
                logInfo(TAG, "onAdEnd: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Closed(ad))
            }

            override fun onAdLeftApplication(baseAd: BaseAd) {}
        }
        interstitialAd.load(adParams.payload)
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            interstitialAd?.play()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        interstitialAd?.adListener = null
        interstitialAd = null
    }
}

private const val TAG = "VungleInterstitialImpl"