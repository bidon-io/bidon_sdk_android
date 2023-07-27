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
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

// $0.1 ca-app-pub-9630071911882835/9299488830
// $0.5 ca-app-pub-9630071911882835/4234864416
// $1.0 ca-app-pub-9630071911882835/7790966049
// $2.0 ca-app-pub-9630071911882835/1445049547

internal class AdmobInterstitialImpl :
    AdSource.Interstitial<AdmobFullscreenAdAuctionParams>,
    AdLoadingType.Network<AdmobFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var param: AdmobFullscreenAdAuctionParams? = null
    private var interstitialAd: InterstitialAd? = null
    private val requiredInterstitialAd: InterstitialAd get() = requireNotNull(interstitialAd)

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            emitEvent(
                AdEvent.PaidRevenue(
                    ad = Ad(
                        demandAd = demandAd,
                        ecpm = param?.lineItem?.pricefloor ?: 0.0,
                        demandAdObject = requiredInterstitialAd,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = "USD",
                        auctionId = auctionId,
                        adUnitId = param?.lineItem?.adUnitId
                    ),
                    adValue = adValue.asBidonAdValue()
                )
            )
        }
    }

    private val interstitialListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked: $this")
                emitEvent(AdEvent.Clicked(requiredInterstitialAd.asAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInfo(TAG, "onAdDismissedFullScreenContent: $this")
                emitEvent(AdEvent.Closed(requiredInterstitialAd.asAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(TAG, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                emitEvent(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInfo(TAG, "onAdShown: $this")
                emitEvent(AdEvent.Shown(requiredInterstitialAd.asAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

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
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed)
            AdmobFullscreenAdAuctionParams(
                lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
                pricefloor = pricefloor,
                context = activity.applicationContext
            )
        }
    }

    override fun fill(adParams: AdmobFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        param = adParams
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
            .build()
        val adUnitId = param?.lineItem?.adUnitId
        if (!adUnitId.isNullOrBlank()) {
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
                    interstitialAd.onPaidEventListener = paidListener
                    interstitialAd.fullScreenContentCallback = interstitialListener
                    emitEvent(AdEvent.Fill(requireNotNull(interstitialAd.asAd())))
                }
            }
            InterstitialAd.load(adParams.context, adUnitId, adRequest, requestListener)
        } else {
            val error = BidonError.NoAppropriateAdUnitId
            logError(
                tag = TAG,
                message = "No appropriate AdUnitId found. PriceFloor=${adParams.pricefloor}, " +
                    "but LineItem with max pricefloor=${param?.lineItem?.pricefloor}",
                error = error
            )
            emitEvent(AdEvent.LoadFailed(error))
        }
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
            adUnitId = param?.lineItem?.adUnitId
        )
    }
}

private const val TAG = "AdmobInterstitial"
