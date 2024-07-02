package org.bidon.gam.impl

import android.app.Activity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.gam.GamInitParameters
import org.bidon.gam.asBidonError
import org.bidon.gam.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class GamInterstitialImpl(
    configParams: GamInitParameters?,
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(configParams),
    private val getFullScreenContentCallback: GetFullScreenContentCallbackUseCase = GetFullScreenContentCallbackUseCase(),
    private val obtainAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Interstitial<GamFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: AdManagerInterstitialAd? = null
    private var price: Double? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams(auctionParamsScope, AdType.Interstitial)
    }

    override fun load(adParams: GamFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adUnitId = when (adParams) {
            is GamFullscreenAdAuctionParams.Bidding -> adParams.adUnitId
            is GamFullscreenAdAuctionParams.Network -> adParams.adUnitId
        } ?: run {
            AdEvent.LoadFailed(
                BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")
            )
            return
        }
        val adRequest = getAdRequest(adParams) ?: run {
            AdEvent.LoadFailed(
                BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
            )
            return
        }
        price = adParams.price
        val requestListener = object : AdManagerInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logInfo(TAG, "onAdFailedToLoad: $loadAdError. $this")
                emitEvent(AdEvent.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                logInfo(TAG, "onAdLoaded: $this")
                this@GamInterstitialImpl.interstitialAd = interstitialAd
                adParams.activity.runOnUiThread {
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        getAd()?.let {
                            emitEvent(AdEvent.PaidRevenue(it, adValue.asBidonAdValue()))
                        }
                    }
                    interstitialAd.fullScreenContentCallback = getFullScreenContentCallback.createCallback(
                        adEventFlow = this@GamInterstitialImpl,
                        getAd = {
                            getAd()
                        },
                        onClosed = {
                            this@GamInterstitialImpl.interstitialAd = null
                        }
                    )
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }
        }
        AdManagerInterstitialAd.load(adParams.activity, adUnitId, adRequest, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            interstitialAd?.show(activity)
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
    }
}

private const val TAG = "GamInterstitial"
