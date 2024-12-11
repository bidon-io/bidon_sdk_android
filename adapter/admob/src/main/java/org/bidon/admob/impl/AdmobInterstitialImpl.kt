package org.bidon.admob.impl

import android.app.Activity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AdmobInterstitialImpl(
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(),
    private val getFullScreenContentCallback: GetFullScreenContentCallbackUseCase = GetFullScreenContentCallbackUseCase(),
    private val obtainAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Interstitial<AdmobFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: InterstitialAd? = null
    private var price: Double? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return obtainAdAuctionParams(auctionParamsScope, demandAd.adType)
    }

    override fun load(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adUnitId = when (adParams) {
            is AdmobFullscreenAdAuctionParams.Network -> adParams.adUnitId
        } ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")
                )
            )
            return
        }
        price = adParams.price
        val requestListener = object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logInfo(TAG, "onAdFailedToLoad: $loadAdError. $this")
                emitEvent(AdEvent.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInfo(TAG, "onAdLoaded: $this")
                this@AdmobInterstitialImpl.interstitialAd = interstitialAd
                adParams.activity.runOnUiThread {
                    interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        getAd()?.let {
                            emitEvent(AdEvent.PaidRevenue(it, adValue.asBidonAdValue()))
                        }
                    }
                    interstitialAd.fullScreenContentCallback = getFullScreenContentCallback.createCallback(
                        adEventFlow = this@AdmobInterstitialImpl,
                        getAd = {
                            getAd()
                        },
                        onClosed = {
                            this@AdmobInterstitialImpl.interstitialAd = null
                        }
                    )
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }
        }
        InterstitialAd.load(adParams.activity, adUnitId, getAdRequest(), requestListener)
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

private const val TAG = "AdmobInterstitial"
