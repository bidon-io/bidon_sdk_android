package org.bidon.sdk.ads.interstitial

import android.app.Activity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.BidOnSdk
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.asUnspecified
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionHolder
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get

internal class InterstitialAdImpl(
    override val placementId: String,
    dispatcher: CoroutineDispatcher = SdkDispatchers.Main,
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
    private val scope by lazy {
        CoroutineScope(dispatcher)
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidOnSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement=$placementId, pricefloor=$pricefloor")
        if (!auctionHolder.isActive) {
            listener.onAuctionStarted()
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Interstitial(
                    activity = activity,
                    pricefloor = pricefloor
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
        return auctionHolder.isAdReady()
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Interstitial<*>)
        observeCallbacksJob = adSource.adEvent.onEach { adEvent ->
            when (adEvent) {
                is AdEvent.Bid,
                is AdEvent.OnReward,
                is AdEvent.Fill -> {
                    // do nothing
                }
                is AdEvent.Clicked -> {
                    sendStatsClickedAsync(adSource)
                    listener.onAdClicked(adEvent.ad)
                }
                is AdEvent.Closed -> listener.onAdClosed(adEvent.ad)
                is AdEvent.Shown -> {
                    sendStatsShownAsync(adSource)
                    listener.onAdShown(adEvent.ad)
                }
                is AdEvent.PaidRevenue -> listener.onRevenuePaid(adEvent.ad, adEvent.adValue)
                is AdEvent.ShowFailed -> listener.onAdShowFailed(adEvent.cause)
                is AdEvent.LoadFailed -> listener.onAdLoadFailed(adEvent.cause)
                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
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

        override fun onRoundStarted(roundId: String, pricefloor: Double) {
            userListener?.onRoundStarted(roundId, pricefloor)
        }

        override fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {
            userListener?.onRoundSucceed(roundId, roundResults)
        }

        override fun onRoundFailed(roundId: String, error: Throwable) {
            userListener?.onRoundFailed(roundId, error)
        }

        override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
            userListener?.onRevenuePaid(ad, adValue)
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
