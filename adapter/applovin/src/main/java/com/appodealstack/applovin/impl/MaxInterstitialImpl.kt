package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appodealstack.applovin.ApplovinFullscreenAdParams
import com.appodealstack.applovin.ApplovinMaxDemandId
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.AdSource.Interstitial.State
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class MaxInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Interstitial<ApplovinFullscreenAdParams> {

    private var interstitialAd: MaxInterstitialAd? = null
    private var maxAd: MaxAd? = null

    private val maxAdListener by lazy {
        logInternal("Applovin", "maxAdListener")
        object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                logInternal("Applovin", "onAdLoaded: $ad")
                maxAd = ad
                state.value = State.Bid.Success(
                    AuctionResult(
                        priceFloor = ad.revenue,
                        adSource = this@MaxInterstitialImpl
                    )
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                state.value = State.Bid.Failure(error.asBidonError())
            }

            override fun onAdDisplayed(ad: MaxAd) {
                logInternal("Applovin", "onAdDisplayed: $ad")
                maxAd = ad
                state.value = State.Show.Impression(ad.asAd())
            }

            override fun onAdHidden(ad: MaxAd) {
                logInternal("Applovin", "onAdHidden: $ad")
                maxAd = ad
                state.value = State.Show.Closed(ad.asAd())
            }

            override fun onAdClicked(ad: MaxAd) {
                logInternal("Applovin", "onAdClicked: $ad")
                maxAd = ad
                state.value = State.Show.Clicked(ad.asAd())
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                logInternal("Applovin", "onAdDisplayFailed: $ad")
                maxAd = ad
                state.value = State.Show.ShowFailed(error.asBidonError())
            }
        }
    }

    override val state = MutableStateFlow<State>(State.Initialized)

    override val ad: Ad?
    get() = maxAd?.asAd() ?: interstitialAd?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
        interstitialAd?.setListener(null)
        interstitialAd?.destroy()
        interstitialAd = null
        maxAd = null
    }

    override fun getParams(priceFloor: Double, timeout: Long, lineItems: List<LineItem>): AdSource.AdParams {
        return ApplovinFullscreenAdParams(
            adUnitId = checkNotNull(lineItems.first { it.demandId == demandId.demandId }.adUnitId),
            timeoutMs = timeout
        )
    }

    override suspend fun bid(
        activity: Activity?,
        adParams: ApplovinFullscreenAdParams
    ): Result<State.Bid.Success> {
        state.value = State.Bid.Requesting
        logInternal(Tag, "Starting with ${adParams.adUnitId}")
        val maxInterstitialAd = MaxInterstitialAd(adParams.adUnitId, activity).also {
            it.setListener(maxAdListener)
            interstitialAd = it
        }
        logInternal("Applovin", "2")
        maxInterstitialAd.loadAd()
        logInternal("Applovin", "3")
        val state = state.first {
            logInternal("Applovin", "state: ${state.value}")
            it is State.Bid.Success || it is State.Bid.Failure
        } as State.Bid
        logInternal("Applovin", "4")
        return when (state) {
            is State.Bid.Failure -> state.cause.asFailure()
            is State.Bid.Success -> state.asSuccess()
            State.Bid.Requesting -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<State.Fill.Success> = runCatching {
        /**
         * Applovin fill the bid automatically. It's not needed to fill it manually.
         */
        State.Fill.Success(
            requireNotNull(interstitialAd?.asAd())
        ).also { state.value = it }
    }

    override fun show(activity: Activity) {
        if (interstitialAd?.isReady == true) {
            interstitialAd?.showAd()
        } else {
            state.value = State.Show.ShowFailed(BidonError.FullscreenAdNotReady)
        }
    }

    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinMaxDemandId,
            demandAd = demandAd,
            price = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = "USD"
        )
    }

    private fun MaxInterstitialAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinMaxDemandId,
            demandAd = demandAd,
            price = 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = null,
            dsp = null,
            roundId = roundId,
            currencyCode = null
        )
    }
}

private const val Tag = "ApplovinMax Interstitial"