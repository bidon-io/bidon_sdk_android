package com.appodealstack.admob.impl

import android.app.Activity
import com.appodealstack.admob.AdmobFullscreenAdAuctionParams
import com.appodealstack.admob.AdmobLineItem
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// $0.1 ca-app-pub-9630071911882835/9299488830
// $0.5 ca-app-pub-9630071911882835/4234864416
// $1.0 ca-app-pub-9630071911882835/7790966049
// $2.0 ca-app-pub-9630071911882835/1445049547

internal class AdmobInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Interstitial<AdmobFullscreenAdAuctionParams> {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
    private val admobLineItems = mutableListOf<AdmobLineItem>()
    private var interstitialAd: InterstitialAd? = null
    private val requiredInterstitialAd: InterstitialAd get() = requireNotNull(interstitialAd)

    private val requestListener by lazy {
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError")
                state.value = AdState.LoadFailed(loadAdError.asBidonError())
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@AdmobInterstitialImpl.interstitialAd = interstitialAd
                interstitialAd.onPaidEventListener = paidListener
                interstitialAd.fullScreenContentCallback = interstitialListener
                state.value = AdState.Bid(
                    AuctionResult(
                        priceFloor = admobLineItems.getPriceFloor(interstitialAd.adUnitId),
                        adSource = this@AdmobInterstitialImpl
                    )
                )
            }
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            val type = when (adValue.precisionType) {
                0 -> "UNKNOWN"
                1 -> "PRECISE"
                2 -> "ESTIMATED"
                3 -> "PUBLISHER_PROVIDED"
                else -> "unknown type ${adValue.precisionType}"
            }
            val valueMicros = adValue.valueMicros
            val ecpm = adValue.valueMicros / 1_000_000L
            logInfo(
                Tag,
                "OnPaidEventListener( ValueMicros=$valueMicros, $ecpm ${adValue.currencyCode}, $type )"
            )
        }
    }

    private val interstitialListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                state.value = AdState.Clicked(requiredInterstitialAd.asAd())
            }

            override fun onAdDismissedFullScreenContent() {
                state.value = AdState.Closed(requiredInterstitialAd.asAd())
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                state.value = AdState.ShowFailed(error.asBidonError())
            }

            override fun onAdImpression() {
                state.value = AdState.Impression(requiredInterstitialAd.asAd())
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

    override val ad: Ad?
        get() = interstitialAd?.asAd()

    override val state = MutableStateFlow<AdState>(AdState.Initialized)

    override fun destroy() {
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
        admobLineItems.clear()
    }

    override fun getAuctionParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdAuctionParams {
        return AdmobFullscreenAdAuctionParams(
            admobLineItems = lineItems
                .filter { it.demandId == demandId.demandId }
                .mapNotNull {
                    val price = it.priceFloor ?: return@mapNotNull null
                    val adUnitId = it.adUnitId ?: return@mapNotNull null
                    AdmobLineItem(price = price, adUnitId = adUnitId)
                }.sortedBy { it.price },
            priceFloor = priceFloor
        )
    }

    override suspend fun bid(activity: Activity?, adParams: AdmobFullscreenAdAuctionParams): Result<AuctionResult> {
        return withContext(dispatcher) {
            logInternal(Tag, "Starting with $adParams")
            admobLineItems.addAll(adParams.admobLineItems)
            val context = activity?.applicationContext
            if (context == null) {
                logError(Tag, "Can not bid() Admob, cause no Context found", BidonError.NoContextFound)
                state.value = AdState.LoadFailed(BidonError.NoContextFound)
            } else {
                val adRequest = AdRequest.Builder().build()
                val adUnitId = admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
                if (!adUnitId.isNullOrBlank()) {
                    InterstitialAd.load(context, adUnitId, adRequest, requestListener)
                } else {
                    val error = BidonError.NoAppropriateAdUnitId
                    logError(
                        tag = Tag,
                        message = "No appropriate AdUnitId found. PriceFloor=${adParams.priceFloor}, " +
                                "but LineItem with max priceFloor=${admobLineItems.last().price}. LineItems: $admobLineItems",
                        error = error
                    )
                    state.value = AdState.LoadFailed(error)
                }
            }
            val state = state.first {
                it is AdState.Bid || it is AdState.LoadFailed
            }
            when (state) {
                is AdState.LoadFailed -> state.cause.asFailure()
                is AdState.Bid -> state.result.asSuccess()
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(interstitialAd?.asAd())
        ).also { state.value = it }.ad
    }

    override fun show(activity: Activity) {
        if (interstitialAd == null) {
            state.value = AdState.ShowFailed(BidonError.FullscreenAdNotReady)
        } else {
            interstitialAd?.show(activity)
        }
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = admobLineItems.getPriceFloor(adUnitId),
            sourceAd = this,
            monetizationNetwork = BNMediationNetwork.GoogleAdmob.networkName,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD"
        )
    }

    private fun List<AdmobLineItem>.getPriceFloor(adUnitId: String): Double {
        return this.first { it.adUnitId == adUnitId }.price
    }
}

private const val Tag = "Admob Interstitial"