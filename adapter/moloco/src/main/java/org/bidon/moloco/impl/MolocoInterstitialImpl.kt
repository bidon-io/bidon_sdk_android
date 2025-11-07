package org.bidon.moloco.impl

import android.app.Activity
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.InterstitialAdShowListener
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import org.bidon.moloco.MolocoDemandId
import org.bidon.moloco.ext.toBidonLoadError
import org.bidon.moloco.ext.toBidonShowError
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

internal class MolocoInterstitialImpl :
    AdSource.Interstitial<MolocoFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null
    override val isAdReadyToShow
        get() = interstitialAd?.isLoaded == true

    private val loadListener = object : AdLoad.Listener {
        override fun onAdLoadSuccess(molocoAd: MolocoAd) {
            logInfo(TAG, "onAdLoadSuccess")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
            val cause = molocoAdError.toBidonLoadError()
            logError(TAG, "onAdLoadFailed", cause)
            emitEvent(AdEvent.LoadFailed(cause))
        }
    }

    private val showListener: InterstitialAdShowListener by lazy {
        object : InterstitialAdShowListener {
            override fun onAdShowSuccess(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdRendered")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = AdValue(
                                adRevenue = molocoAd.revenue?.toDouble() ?: 0.0,
                                currency = AdValue.USD,
                                precision = Precision.Precise
                            )
                        )
                    )
                }
            }

            override fun onAdShowFailed(molocoAdError: MolocoAdError) {
                logInfo(TAG, "onAdShowFailed: ${molocoAdError.description}")
                emitEvent(AdEvent.ShowFailed(molocoAdError.toBidonShowError()))
            }

            override fun onAdHidden(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdHidden: $this")
                getAd()?.let { emitEvent(AdEvent.Closed(it)) }
            }

            override fun onAdClicked(molocoAd: MolocoAd) {
                logInfo(TAG, "onAdClicked")
                getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
            }
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            interstitialAd?.show(showListener)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun load(adParams: MolocoFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.adUnitId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")
                )
            )
            return
        }
        adParams.payload ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                )
            )
            return
        }
        Moloco.createInterstitial(
            adUnitId = adParams.adUnitId
        ) { interstitial: InterstitialAd?, adCreateError: MolocoAdError.AdCreateError? ->
            if (interstitial != null) {
                interstitialAd = interstitial
                interstitial.load(adParams.payload, listener = loadListener)
            } else {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.Unspecified(
                            MolocoDemandId,
                            message = adCreateError?.description ?: "Created interstitial is null."
                        )
                    )
                )
            }
        }
    }

    override fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }
}

private const val TAG = "MolocoInterstitialImpl"
