package com.appodealstack.admob.impl

import android.app.Activity
import com.appodealstack.admob.AdmobFullscreenAdAuctionParams
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.data.models.minByPricefloorOrNull
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var lineItem: LineItem? = null
    private var interstitialAd: InterstitialAd? = null
    private val requiredInterstitialAd: InterstitialAd get() = requireNotNull(interstitialAd)

    private val requestListener by lazy {
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logInternal(Tag, "onAdFailedToLoad: $loadAdError. $this", loadAdError.asBidonError())
                adState.tryEmit(AdState.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInternal(Tag, "onAdLoaded: $this")
                this@AdmobInterstitialImpl.interstitialAd = interstitialAd
                interstitialAd.onPaidEventListener = paidListener
                interstitialAd.fullScreenContentCallback = interstitialListener
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            priceFloor = requireNotNull(lineItem?.priceFloor),
                            adSource = this@AdmobInterstitialImpl
                        )
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
            logInternal(
                tag = Tag,
                message = "OnPaidEventListener( ValueMicros=$valueMicros, $ecpm ${adValue.currencyCode}, $type ). $this"
            )
        }
    }

    private val interstitialListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInternal(Tag, "onAdClicked: $this")
                adState.tryEmit(AdState.Clicked(requiredInterstitialAd.asAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInternal(Tag, "onAdDismissedFullScreenContent: $this")
                adState.tryEmit(AdState.Closed(requiredInterstitialAd.asAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(Tag, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adState.tryEmit(AdState.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInternal(Tag, "onAdImpression: $this")
                adState.tryEmit(AdState.Impression(requiredInterstitialAd.asAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

    override val ad: Ad?
        get() = interstitialAd?.asAd()

    override val adState = MutableSharedFlow<AdState>(Int.MAX_VALUE)

    override fun destroy() {
        logInternal(Tag, "destroy $this")
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
        lineItem = null
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
            ?.also(onLineItemConsumed)
        AdmobFullscreenAdAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            priceFloor = priceFloor,
            context = activity.applicationContext
        )
    }

    override suspend fun bid(adParams: AdmobFullscreenAdAuctionParams): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams: $this")
        return withContext(dispatcher) {
            lineItem = adParams.lineItem
            val adRequest = AdRequest.Builder().build()
            val adUnitId = lineItem?.adUnitId
            if (!adUnitId.isNullOrBlank()) {
                InterstitialAd.load(adParams.context, adUnitId, adRequest, requestListener)
            } else {
                val error = BidonError.NoAppropriateAdUnitId
                logError(
                    tag = Tag,
                    message = "No appropriate AdUnitId found. PriceFloor=${adParams.priceFloor}, " +
                        "but LineItem with max priceFloor=${lineItem?.priceFloor}",
                    error = error
                )
                adState.tryEmit(AdState.LoadFailed(error))
            }
            val state = adState.first {
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
        logInternal(Tag, "Starting fill: $this")
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(interstitialAd?.asAd())
        ).also { adState.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {
        logInternal(Tag, "Starting show: $this")
        if (interstitialAd == null) {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            interstitialAd?.show(activity)
        }
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD"
        )
    }
}

private const val Tag = "Admob Interstitial"
