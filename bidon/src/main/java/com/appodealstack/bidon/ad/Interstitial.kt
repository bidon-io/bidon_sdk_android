package com.appodealstack.bidon.ad

import android.app.Activity
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.AdState
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class Interstitial(
    override val placementId: String = DefaultPlacement
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun load()
    fun destroy()
    fun show(activity: Activity)
    fun setInterstitialListener(listener: InterstitialListener)
}

internal class InterstitialAdImpl(
    override val placementId: String,
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Default,
) : InterstitialAd {

    private val demandAd by lazy {
        DemandAd(AdType.Interstitial, placementId)
    }
    private var auction: Auction? = null
    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private var userListener: InterstitialListener? = null
    private var auctionJob: Job? = null
    private var observeCallbacksJob: Job? = null

    private val listener by lazy {
        getInterstitialListener()
    }

    override fun load() {
        logInfo(Tag, "Load with placement: $placementId")
        if (auctionJob?.isActive == true) return

        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
        listener.auctionStarted()

        auctionJob = scope.launch {
            val auction = get<Auction>().also {
                auction = it
            }
            auction.start(
                demandAd = demandAd,
                resolver = MaxEcpmAuctionResolver,
                adTypeAdditionalData = AdTypeAdditional.Interstitial(
                    activity = null
                ),
                roundsListener = listener
            ).onSuccess { results ->
                logInfo(Tag, "Auction completed successfully: $results")

                val winner = results.first()
                subscribeToWinner(winner.adSource)
                listener.auctionSucceed(results)
                listener.onAdLoaded(
                    requireNotNull(winner.adSource.ad) {
                        "[Ad] should exist when the Action succeeds"
                    }
                )
            }.onFailure {
                logError(Tag, "Auction failed", it)
                listener.auctionFailed(error = it)
                listener.onAdLoadFailed(cause = it)
            }
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Show with placement: $placementId")
        val auction = auction
        when {
            auctionJob?.isActive == true -> {
                logInfo(Tag, "Show failed. Auction in progress.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
            auction == null -> {
                logInfo(Tag, "Show failed. No completed Auction.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
            auction.results.isEmpty() -> {
                logInfo(Tag, "Show failed. No Auction results.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
            else -> {
                auction.results.map { it.adSource }
                    .filterIsInstance<AdSource.Interstitial<*>>()
                    .first()
                    .show(activity)
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(Tag, "Set interstitial listener")
        this.userListener = listener
    }

    override fun destroy() {
        auctionJob?.cancel()
        auctionJob = null
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
        auction?.destroy()
        auction = null
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Interstitial<*>)
        observeCallbacksJob = adSource.state.onEach { state ->
            when (state) {
                AdState.Initialized,
                is AdState.Bid ,
                is AdState.OnReward,
                is AdState.Fill -> {
                    // do nothing
                }
                is AdState.Clicked -> listener.onAdClicked(state.ad)
                is AdState.Closed -> listener.onAdClosed(state.ad)
                is AdState.Impression -> listener.onAdImpression(state.ad)
                is AdState.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdState.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdState.Expired -> listener.onAdExpired(state.ad)
            }
        }.launchIn(scope)
    }

    private fun getInterstitialListener() = object : InterstitialListener {
        override fun onAdLoaded(ad: Ad) {
            userListener?.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: Throwable) {
            userListener?.onAdLoadFailed(cause)
        }

        override fun onAdShowFailed(cause: Throwable) {
            userListener?.onAdShowFailed(cause)
        }

        override fun onAdImpression(ad: Ad) {
            BidON.logRevenue(ad)
            userListener?.onAdImpression(ad)
        }

        override fun onAdClicked(ad: Ad) {
            userListener?.onAdClicked(ad)
        }

        override fun onAdClosed(ad: Ad) {
            userListener?.onAdClosed(ad)
        }

        override fun onAdExpired(ad: Ad) {
            userListener?.onAdExpired(ad)
        }

        override fun auctionStarted() {
            userListener?.auctionStarted()
        }

        override fun auctionSucceed(auctionResults: List<AuctionResult>) {
            userListener?.auctionSucceed(auctionResults)
        }

        override fun auctionFailed(error: Throwable) {
            userListener?.auctionFailed(error)
        }

        override fun roundStarted(roundId: String) {
            userListener?.roundStarted(roundId)
        }

        override fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {
            userListener?.roundSucceed(roundId, roundResults)
        }

        override fun roundFailed(roundId: String, error: Throwable) {
            userListener?.roundFailed(roundId, error)
        }
    }

}

private const val Tag = "Interstitial"
