package org.bidon.admob.impl

import android.app.Activity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class AdmobInterstitialImpl :
    AdSource.Interstitial<AdmobFullscreenAdAuctionParams>,
    AdLoadingType.Network<AdmobFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: AdmobFullscreenAdAuctionParams? = null
    private var interstitialAd: InterstitialAd? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
        param = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            AdmobFullscreenAdAuctionParams(
                lineItem = lineItem,
                context = activity.applicationContext,
                adUnitId = requireNotNull(lineItem.adUnitId)
            )
        }
    }

    override fun fill(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        param = adParams
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
            .build()
        val adUnitId = adParams.adUnitId
        val requestListener = object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(
                    TAG,
                    "onAdFailedToLoad: $loadAdError. $this",
                    loadAdError.asBidonError()
                )
                emitEvent(AdEvent.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInfo(TAG, "onAdLoaded: $this")
                this@AdmobInterstitialImpl.interstitialAd = interstitialAd
                interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = Ad(
                                demandAd = demandAd,
                                ecpm = adParams.lineItem.pricefloor,
                                demandAdObject = interstitialAd,
                                networkName = demandId.demandId,
                                dsp = null,
                                roundId = roundId,
                                currencyCode = "USD",
                                auctionId = auctionId,
                                adUnitId = adParams.lineItem.adUnitId
                            ),
                            adValue = adValue.asBidonAdValue()
                        )
                    )
                }
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        emitEvent(AdEvent.Clicked(interstitialAd.asAd()))
                    }

                    override fun onAdDismissedFullScreenContent() {
                        logInfo(TAG, "onAdDismissedFullScreenContent: $this")
                        emitEvent(AdEvent.Closed(interstitialAd.asAd()))
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        logError(TAG, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                        emitEvent(AdEvent.ShowFailed(error.asBidonError()))
                    }

                    override fun onAdImpression() {
                        logInfo(TAG, "onAdShown: $this")
                        emitEvent(AdEvent.Shown(interstitialAd.asAd()))
                    }

                    override fun onAdShowedFullScreenContent() {}
                }
                emitEvent(AdEvent.Fill(requireNotNull(interstitialAd.asAd())))
            }
        }
        InterstitialAd.load(adParams.context, adUnitId, adRequest, requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            interstitialAd?.show(activity)
        }
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
            adUnitId = param?.adUnitId
        )
    }
}

private const val TAG = "AdmobInterstitial"
