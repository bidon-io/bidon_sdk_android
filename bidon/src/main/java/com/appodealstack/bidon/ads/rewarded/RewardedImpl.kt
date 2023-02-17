package com.appodealstack.bidon.ads.rewarded

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.adapter.AdEvent
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.ads.*
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

internal class RewardedImpl(
    override val placementId: String,
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Default,
) : Rewarded {

    private val demandAd by lazy {
        DemandAd(AdType.Rewarded, placementId)
    }
    private var userListener: RewardedListener? = null
    private var observeCallbacksJob: Job? = null
    private val auctionHolder: AuctionHolder by lazy {
        com.appodealstack.bidon.utils.di.get { params(demandAd, listener) }
    }

    private val listener by lazy {
        getRewardedListener()
    }

    override fun isReady(): Boolean {
        return auctionHolder.isAdReady()
    }

    override fun loadAd(activity: Activity, minPrice: Double) {
        if (!BidOnSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement: $placementId")
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null

        if (!auctionHolder.isActive) {
            listener.onAuctionStarted()
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Rewarded(
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
                require(adSource is AdSource.Rewarded<*>) {
                    "Unexpected AdSource type. Expected: AdSource.Rewarded. Actual: ${adSource::class.java}."
                }
                adSource.show(activity)
            }
        }
    }

    override fun setRewardedListener(listener: RewardedListener) {
        logInfo(Tag, "Set rewarded listener")
        this.userListener = listener
    }

    override fun destroyAd() {
        auctionHolder.destroy()
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Rewarded<*>)
        observeCallbacksJob = adSource.adEvent.onEach { state ->
            when (state) {
                is AdEvent.Bid,
                is AdEvent.Fill -> {
                    // do nothing
                }
                is AdEvent.OnReward -> {
                    sendStatsRewardAsync(adSource)
                    listener.onUserRewarded(state.ad, state.reward)
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

    private fun getRewardedListener() = object : RewardedListener {
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

        override fun onUserRewarded(ad: Ad, reward: Reward?) {
            userListener?.onUserRewarded(ad, reward)
        }

        override fun onRevenuePaid(ad: Ad) {
            userListener?.onRevenuePaid(ad)
        }
    }

    private suspend fun sendStatsClickedAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendClickImpression(
                    StatisticsCollector.AdType.Rewarded
                )
            }
        }
    }

    private suspend fun sendStatsShownAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendShowImpression(
                    StatisticsCollector.AdType.Rewarded
                )
            }
        }
    }

    private suspend fun sendStatsRewardAsync(adSource: AdSource.Rewarded<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendRewardImpression()
            }
        }
    }
}

private const val Tag = "Rewarded"
