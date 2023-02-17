package com.appodealstack.bidon.ads.interstitial

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.adapter.AdEvent
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.ads.asUnspecified
import com.appodealstack.bidon.auction.AdTypeParam
import com.appodealstack.bidon.auction.AuctionHolder
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.config.BidonError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.utils.SdkDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
        com.appodealstack.bidon.utils.di.get {
            params(demandAd, listener)
        }
    }

    private val listener by lazy {
        getInterstitialListener()
    }

    override fun loadAd(activity: Activity, minPrice: Double) {
        if (!BidOnSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement=$placementId, minPrice=$minPrice")
        if (!auctionHolder.isActive) {
            listener.onAuctionStarted()
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Interstitial(
                    activity = activity,
                    priceFloor = minPrice
                ),
                onResult = { result ->
                    result
                        .onSuccess { auctionResults ->
                            /**
                             * Winner found
                             */
                            val winner = auctionResults.first()
                            subscribeToWinner(winner.adSource)
                            listener.onAuctionSuccess(auctionResults)
                            listener.onAdLoaded(
                                requireNotNull(winner.adSource.ad) {
                                    "[Ad] should exist when the Action succeeds"
                                }
                            )
                        }.onFailure {
                            /**
                             * Auction failed
                             */
                            listener.onAuctionFailed(error = it)
                            listener.onAdLoadFailed(cause = it.asUnspecified())
                        }
                }
            )
        } else {
            logInfo(Tag, "Auction already in progress. Placement: $placementId.")
        }
    }

    override fun showAd(activity: Activity) {
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

    override fun destroyAd() {
        auctionHolder.destroy()
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
    }

    override fun isReady(): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Interstitial<*>)
        observeCallbacksJob = adSource.adEvent.onEach { state ->
            when (state) {
                is AdEvent.Bid,
                is AdEvent.OnReward,
                is AdEvent.Fill,
                -> {
                    // do nothing
                }
                is AdEvent.Clicked -> {
                    sendStatsClickedAsync(adSource)
                    listener.onAdClicked(state.ad)
                }
                is AdEvent.Closed -> listener.onAdClosed(state.ad)
                is AdEvent.Shown -> {
                    sendStatsShownAsync(adSource)
                    listener.onAdShown(state.ad)
                }
                is AdEvent.PaidRevenue -> listener.onRevenuePaid(state.ad)
                is AdEvent.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdEvent.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdEvent.Expired -> listener.onAdExpired(state.ad)
            }
        }.launchIn(CoroutineScope(dispatcher))
    }

    private fun getInterstitialListener() = object : InterstitialListener {
        override fun onAdLoaded(ad: Ad) {
            userListener?.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: BidonError) {
            userListener?.onAdLoadFailed(cause)
        }

        override fun onAdShowFailed(cause: BidonError) {
            userListener?.onAdShowFailed(cause)
        }

        override fun onAdShown(ad: Ad) {
            userListener?.onAdShown(ad)
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

        override fun onAuctionStarted() {
            userListener?.onAuctionStarted()
        }

        override fun onAuctionSuccess(auctionResults: List<AuctionResult>) {
            userListener?.onAuctionSuccess(auctionResults)
        }

        override fun onAuctionFailed(error: Throwable) {
            userListener?.onAuctionFailed(error)
        }

        override fun onRoundStarted(roundId: String, priceFloor: Double) {
            userListener?.onRoundStarted(roundId, priceFloor)
        }

        override fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {
            userListener?.onRoundSucceed(roundId, roundResults)
        }

        override fun onRoundFailed(roundId: String, error: Throwable) {
            userListener?.onRoundFailed(roundId, error)
        }

        override fun onRevenuePaid(ad: Ad) {
            userListener?.onRevenuePaid(ad)
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
