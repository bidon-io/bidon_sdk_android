package org.bidon.vungle.impl

import android.app.Activity
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.BaseAdListener
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleError
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
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleInterstitialImpl :
    AdSource.Interstitial<VungleFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.canPlayAd() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleFullscreenAuctionParams(
                activity = auctionParamsScope.activity,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: VungleFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        val placementId = adParams.placementId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")))

        val interstitialAd = InterstitialAd(adParams.activity, placementId, AdConfig())
            .also { interstitialAd = it }
        interstitialAd.adListener = object : BaseAdListener {
            override fun onAdLoaded(baseAd: BaseAd) {
                logInfo(TAG, "onAdLoaded placementId=${baseAd.placementId}. $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Fill(ad))
            }

            override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onAdFailedToLoad placementId=${baseAd.placementId}. $this", adError)
                emitEvent(AdEvent.LoadFailed(adError.asBidonError()))
            }

            override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onAdFailedToPlay: $this", adError)
                emitEvent(AdEvent.ShowFailed(adError.asBidonError()))
            }

            override fun onAdStart(baseAd: BaseAd) {
                logInfo(TAG, "onAdStart: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Shown(ad))
            }

            override fun onAdClicked(baseAd: BaseAd) {
                logInfo(TAG, "onAdClicked: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdEnd(baseAd: BaseAd) {
                logInfo(TAG, "onAdEnd: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Closed(ad))
            }

            override fun onAdLeftApplication(baseAd: BaseAd) {
                logInfo(TAG, "onAdLeftApplication: $this")
            }

            override fun onAdImpression(baseAd: BaseAd) {
                logInfo(TAG, "onAdImpression: $this")
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
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            interstitialAd.load(payload)
        } else {
            interstitialAd.load()
        }
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