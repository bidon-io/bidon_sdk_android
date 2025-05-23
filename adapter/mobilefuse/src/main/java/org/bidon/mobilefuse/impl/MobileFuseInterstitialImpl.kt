package org.bidon.mobilefuse.impl

import android.app.Activity
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseInterstitialAd
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
import java.util.concurrent.atomic.AtomicBoolean

internal class MobileFuseInterstitialImpl :
    AdSource.Interstitial<MobileFuseFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: MobileFuseInterstitialAd? = null

    /**
     * This flag is used to prevent [AdError]-callback from being exposed twice.
     */
    private var isLoaded = AtomicBoolean(false)

    override val isAdReadyToShow: Boolean get() = interstitialAd?.isLoaded == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }

    override fun load(adParams: MobileFuseFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            adParams.signalData ?: run {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(demandId = demandId, message = "signalData")
                    )
                )
                return
            }
        }
        // placementId should be configured in the mediation platform UI and passed back to this method:
        val interstitialAd = MobileFuseInterstitialAd(adParams.activity, adParams.placementId).also {
            interstitialAd = it
        }
        interstitialAd.setListener(object : MobileFuseInterstitialAd.Listener {
            override fun onAdLoaded() {
                if (!isLoaded.getAndSet(true)) {
                    logInfo(TAG, "onAdLoaded")
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }

            override fun onAdNotFilled() {
                val cause = BidonError.NoFill(demandId)
                logError(TAG, "onAdNotFilled", cause)
                emitEvent(AdEvent.LoadFailed(cause))
            }

            override fun onAdRendered() {
                logInfo(TAG, "onAdRendered")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = interstitialAd.winningBidInfo.let { bidInfo ->
                                AdValue(
                                    adRevenue = bidInfo?.cpmPrice?.div(1000.0) ?: 0.0,
                                    currency = bidInfo?.currency ?: AdValue.USD,
                                    precision = Precision.Precise
                                )
                            }
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked")
                getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onAdExpired() {
                logInfo(TAG, "onAdExpired")
                emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }

            override fun onAdError(adError: AdError?) {
                logError(TAG, "onAdError $adError", Throwable(adError?.errorMessage))
                when (adError) {
                    AdError.AD_ALREADY_RENDERED -> {
                        emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }

                    AdError.AD_ALREADY_LOADED -> {
                        // do nothing
                    }

                    AdError.AD_LOAD_ERROR -> {
                        if (!isLoaded.getAndSet(true)) {
                            getAd()?.let { emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId))) }
                        }
                    }

                    else -> {
                        emitEvent(
                            AdEvent.LoadFailed(
                                BidonError.Unspecified(
                                    demandId = demandId,
                                    cause = Throwable("Message: ${adError?.errorMessage}. Code: ${adError?.errorCode}")
                                )
                            )
                        )
                    }
                }
            }

            override fun onAdClosed() {
                logInfo(TAG, "onAdClosed: $this")
                getAd()?.let { emitEvent(AdEvent.Closed(it)) }
                this@MobileFuseInterstitialImpl.interstitialAd = null
            }
        })
        interstitialAd.loadAdFromBiddingToken(adParams.signalData)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd?.isLoaded == true) {
            interstitialAd?.showAd()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd = null
    }
}

private const val TAG = "MobileFuseInterstitialImpl"