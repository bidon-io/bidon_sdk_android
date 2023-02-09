package com.appodealstack.bidon.view

import android.app.Activity
import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.AdState
import com.appodealstack.bidon.domain.auction.AdTypeParam
import com.appodealstack.bidon.domain.auction.AuctionHolder
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.helper.SdkDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Interstitial(
    override val placementId: String = DefaultPlacement,
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun load(activity: Activity)
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
    private var userListener: InterstitialListener? = null
    private var observeCallbacksJob: Job? = null
    private val auctionHolder: AuctionHolder by lazy {
        get {
            params(demandAd, listener)
        }
    }

    private val listener by lazy {
        getInterstitialListener()
    }

    override fun load(activity: Activity) {
        if (!BidOn.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement: $placementId")
        if (!auctionHolder.isActive) {
            listener.auctionStarted()
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Interstitial(
                    activity = activity
                ),
                onResult = { result ->
                    result
                        .onSuccess { auctionResults ->
                            /**
                             * Winner found
                             */
                            val winner = auctionResults.first()
                            subscribeToWinner(winner.adSource)
                            listener.auctionSucceed(auctionResults)
                            listener.onAdLoaded(
                                requireNotNull(winner.adSource.ad) {
                                    "[Ad] should exist when the Action succeeds"
                                }
                            )
                        }.onFailure {
                            /**
                             * Auction failed
                             */
                            listener.auctionFailed(error = it)
                            listener.onAdLoadFailed(cause = it)
                        }
                }
            )
        } else {
            logInfo(Tag, "Auction already in progress. Placement: $placementId.")
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Show with placement: $placementId")
        if (auctionHolder.isActive) {
            logInfo(Tag, "Show failed. Auction in progress.")
            listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            return
        }
        when (val adSource = auctionHolder.popWinner()) {
            null -> {
                logInfo(Tag, "Show failed. No Auction results.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
            else -> {
                require(adSource is AdSource.Interstitial<*>) {
                    "Unexpected AdSource type. Expected: AdSource.Interstitial. Actual: ${adSource::class.java}."
                }
                adSource.show(activity)
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(Tag, "Set interstitial listener")
        this.userListener = listener
    }

    override fun destroy() {
        auctionHolder.destroy()
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Interstitial<*>)
        observeCallbacksJob = adSource.adState.onEach { state ->
            when (state) {
                is AdState.Bid,
                is AdState.OnReward,
                is AdState.Fill,
                -> {
                    // do nothing
                }
                is AdState.Clicked -> {
                    sendStatsClickedAsync(adSource)
                    listener.onAdClicked(state.ad)
                }
                is AdState.Closed -> listener.onAdClosed(state.ad)
                is AdState.Impression -> {
                    sendStatsShownAsync(adSource)
                    listener.onAdImpression(state.ad)
                }
                is AdState.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdState.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdState.Expired -> listener.onAdExpired(state.ad)
            }
        }.launchIn(CoroutineScope(dispatcher))
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
            BidOn.logRevenue(ad)
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

    private suspend fun sendStatsClickedAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendClickImpression(
                    StatisticsCollector.AdType.Interstitial
                )
            }
        }
    }

    private suspend fun sendStatsShownAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendShowImpression(
                    StatisticsCollector.AdType.Interstitial
                )
            }
        }
    }
}

private const val Tag = "Interstitial"