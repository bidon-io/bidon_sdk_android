package org.bidon.admob.impl

import android.app.Activity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus
import org.bidon.sdk.utils.SdkDispatchers

// $0.1 ca-app-pub-9630071911882835/9299488830
// $0.5 ca-app-pub-9630071911882835/4234864416
// $1.0 ca-app-pub-9630071911882835/7790966049
// $2.0 ca-app-pub-9630071911882835/1445049547

internal class AdmobInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Interstitial<AdmobFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var lineItem: LineItem? = null
    private var interstitialAd: InterstitialAd? = null
    private val requiredInterstitialAd: InterstitialAd get() = requireNotNull(interstitialAd)

    private val requestListener by lazy {
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "onAdFailedToLoad: $loadAdError. $this", loadAdError.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = loadAdError.asBidonError().asRoundStatus(),
                )
                adEvent.tryEmit(AdEvent.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                logInfo(Tag, "onAdLoaded: $this")
                this@AdmobInterstitialImpl.interstitialAd = interstitialAd
                interstitialAd.onPaidEventListener = paidListener
                interstitialAd.fullScreenContentCallback = interstitialListener
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.pricefloor),
                    roundStatus = RoundStatus.Successful,
                )
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = requireNotNull(lineItem?.pricefloor),
                            adSource = this@AdmobInterstitialImpl,
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
            adEvent.tryEmit(
                AdEvent.PaidRevenue(
                    ad = Ad(
                        demandAd = demandAd,
                        ecpm = lineItem?.pricefloor ?: 0.0,
                        sourceAd = requiredInterstitialAd,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = "USD",
                        auctionId = auctionId,
                        adUnitId = lineItem?.adUnitId
                    ),
                    adValue = adValue.asBidonAdValue()
                )
            )
        }
    }

    private val interstitialListener by lazy {
        object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(requiredInterstitialAd.asAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInfo(Tag, "onAdDismissedFullScreenContent: $this")
                adEvent.tryEmit(AdEvent.Closed(requiredInterstitialAd.asAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(Tag, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInfo(Tag, "onAdShown: $this")
                adEvent.tryEmit(AdEvent.Shown(requiredInterstitialAd.asAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }

    override val ad: Ad?
        get() = interstitialAd?.asAd()

    override val adEvent = MutableSharedFlow<AdEvent>(Int.MAX_VALUE)
    override val isAdReadyToShow: Boolean
        get() = interstitialAd != null

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        interstitialAd?.onPaidEventListener = null
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
        lineItem = null
    }

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed)
        AdmobFullscreenAdAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            pricefloor = pricefloor,
            context = activity.applicationContext
        )
    }

    override suspend fun bid(adParams: AdmobFullscreenAdAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
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
                    message = "No appropriate AdUnitId found. PriceFloor=${adParams.pricefloor}, " +
                        "but LineItem with max pricefloor=${lineItem?.pricefloor}",
                    error = error
                )
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }
            val state = adEvent.first {
                it is AdEvent.Bid || it is AdEvent.LoadFailed
            }
            when (state) {
                is AdEvent.LoadFailed -> {
                    AuctionResult(
                        ecpm = 0.0,
                        adSource = this@AdmobInterstitialImpl
                    )
                }
                is AdEvent.Bid -> state.result
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdEvent.Fill(
            requireNotNull(interstitialAd?.asAd())
        ).also {
            markFillFinished(RoundStatus.Successful)
            adEvent.tryEmit(it)
        }.ad
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (interstitialAd == null) {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            interstitialAd?.show(activity)
        }
    }

    private fun InterstitialAd.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = lineItem?.pricefloor ?: 0.0,
            sourceAd = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }
}

private const val Tag = "Admob Interstitial"
