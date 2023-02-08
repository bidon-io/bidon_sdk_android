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
import com.appodealstack.bidon.domain.common.*
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.helper.SdkDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Rewarded(
    override val placementId: String = DefaultPlacement
) : RewardedAd by RewardedImpl(placementId)

interface RewardedAd {
    val placementId: String

    fun load(activity: Activity)
    fun destroy()
    fun show(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}

internal class RewardedImpl(
    override val placementId: String,
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Default,
) : RewardedAd {

    private val demandAd by lazy {
        DemandAd(AdType.Rewarded, placementId)
    }
    private var userListener: RewardedListener? = null
    private var observeCallbacksJob: Job? = null
    private val auctionHolder: AuctionHolder by lazy {
        get { params(demandAd, listener) }
    }

    private val listener by lazy {
        getRewardedListener()
    }

    override fun load(activity: Activity) {
        if (!BidOn.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement: $placementId")
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null

        if (!auctionHolder.isActive) {
            listener.auctionStarted()
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Rewarded(
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
                            listener.onAdLoadFailed(cause = it.asUnspecified())
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

    override fun destroy() {
        auctionHolder.destroy()
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Rewarded<*>)
        observeCallbacksJob = adSource.adState.onEach { state ->
            when (state) {
                is AdState.Bid,
                is AdState.Fill -> {
                    // do nothing
                }
                is AdState.OnReward -> {
                    sendStatsRewardAsync(adSource)
                    listener.onUserRewarded(state.ad, state.reward)
                }
                is AdState.Clicked -> {
                    sendStatsClickedAsync(adSource)
                    listener.onAdClicked(state.ad)
                }
                is AdState.Closed -> listener.onAdClosed(state.ad)
                is AdState.Impression -> {
                    sendStatsShownAsync(adSource)
                    listener.onAdShown(state.ad)
                }
                is AdState.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdState.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdState.Expired -> listener.onAdExpired(state.ad)
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
            BidOn.logRevenue(ad)
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

        override fun onUserRewarded(ad: Ad, reward: Reward?) {
            userListener?.onUserRewarded(ad, reward)
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
