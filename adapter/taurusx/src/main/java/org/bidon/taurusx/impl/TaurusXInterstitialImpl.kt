package org.bidon.taurusx.impl

import android.app.Activity
import com.taurusx.tax.api.OnTaurusXInterstitialListener
import com.taurusx.tax.api.TaurusXAdError
import com.taurusx.tax.api.TaurusXInterstitialAds
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
import org.bidon.taurusx.ext.asBidonError

internal class TaurusXInterstitialImpl :
    AdSource.Interstitial<TaurusXFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitial: TaurusXInterstitialAds? = null

    private val listener: OnTaurusXInterstitialListener = object : OnTaurusXInterstitialListener {
        override fun onAdLoaded() {
            logInfo(TAG, "Interstitial ad loaded successfully")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onAdShown() {
            logInfo(TAG, "Interstitial ad shown successfully")
            getAd()?.let { ad ->
                emitEvent(AdEvent.Shown(ad))
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = interstitial?.price?.toDouble() ?: 0.0,
                            currency = AdValue.USD,
                            precision = Precision.Precise
                        )
                    )
                )
            }
        }

        override fun onAdClicked() {
            logInfo(TAG, "Interstitial ad clicked")
            getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
        }

        override fun onAdClosed() {
            logInfo(TAG, "Interstitial ad closed")
            getAd()?.let { emitEvent(AdEvent.Closed(it)) }
        }

        override fun onAdFailedToLoad(error: TaurusXAdError) {
            logInfo(TAG, "Interstitial ad load failed: ${error.message}")
            emitEvent(AdEvent.LoadFailed(error.asBidonError()))
        }

        override fun onAdShowFailed(error: TaurusXAdError) {
            logInfo(TAG, "Interstitial ad show failed: ${error.message}")
            emitEvent(AdEvent.ShowFailed(error.asBidonError()))
        }
    }

    override val isAdReadyToShow: Boolean
        get() = interstitial?.isReady == true

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            interstitial?.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun load(adParams: TaurusXFullscreenAuctionParams) {
        logInfo(TAG, "Starting interstitial load")
        val adUnitId = adParams.adUnitId
        if (adUnitId == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(
                        demandId = demandId, message = "adUnitId is required"
                    )
                )
            )
            return
        }
        val bidType = adParams.adUnit.bidType
        val payload = adParams.payload
        if (bidType == BidType.RTB && payload == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "payload is required")
                )
            )
            return
        }
        val interstitial = TaurusXInterstitialAds(adParams.context).also {
            interstitial = it
        }
        interstitial.setAdUnitId(adUnitId)
        interstitial.setListener(listener)
        if (bidType == BidType.RTB) {
            interstitial.loadInterstitialFromBid(payload)
        } else {
            interstitial.loadInterstitial()
        }
    }

    override fun destroy() {
        interstitial?.destroy()
        interstitial = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }
}

private const val TAG = "TaurusXInterstitialImpl"