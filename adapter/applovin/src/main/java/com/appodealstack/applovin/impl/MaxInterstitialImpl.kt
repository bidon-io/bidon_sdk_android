package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.MaxFullscreenAdAuctionParams
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.config.BidonError
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.stats.models.RoundStatus
import com.appodealstack.bidon.stats.models.asRoundStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class MaxInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Interstitial<MaxFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var interstitialAd: MaxInterstitialAd? = null
    private var maxAd: MaxAd? = null

    private val maxAdListener by lazy {
        object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                markBidFinished(
                    ecpm = requireNotNull(ad.revenue),
                    roundStatus = RoundStatus.Successful,
                )
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = ad.revenue,
                            adSource = this@MaxInterstitialImpl,
                        )
                    )
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = error.asBidonError().asRoundStatus(),
                )
                adEvent.tryEmit(AdEvent.LoadFailed(error.asBidonError()))
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isReady == true

    override val ad: Ad?
        get() = maxAd?.asAd() ?: interstitialAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        interstitialAd?.setListener(null)
        interstitialAd?.destroy()
        interstitialAd = null
        maxAd = null
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
        MaxFullscreenAdAuctionParams(
            activity = activity,
            lineItem = requireNotNull(lineItem),
            timeoutMs = timeout,
        )
    }

    override suspend fun bid(adParams: MaxFullscreenAdAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        val maxInterstitialAd = MaxInterstitialAd(adParams.lineItem.adUnitId, adParams.activity).also {
            it.setListener(maxAdListener)
            interstitialAd = it
        }
        maxInterstitialAd.loadAd()
        val state = adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed
        }
        return when (state) {
            is AdEvent.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdEvent.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Applovin fills the bid automatically. It's not needed to fill it manually.
         */
        AdEvent.Fill(
            requireNotNull(interstitialAd?.asAd())
        ).also { adEvent.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {
        if (interstitialAd?.isReady == true) {
            interstitialAd?.showAd()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            price = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            networkName = ApplovinDemandId.demandId,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxInterstitialAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            price = 0.0,
            sourceAd = maxAd ?: demandAd,
            networkName = ApplovinDemandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "Max Interstitial"
private const val USD = "USD"
