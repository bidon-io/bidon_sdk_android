package com.appodealstack.bidon.ad

import android.app.Activity
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.AdSource.State
import com.appodealstack.bidon.adapters.AdSource.Rewarded.OnReward
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

class Rewarded(
    override val placementId: String = DefaultPlacement
) : RewardedAd by RewardedImpl(placementId)

interface RewardedAd {
    val placementId: String

    fun load()
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
    private var auction: Auction? = null
    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private var userListener: RewardedListener? = null
    private var auctionJob: Job? = null
    private var observeCallbacksJob: Job? = null

    private val listener by lazy {
        getRewardedListener()
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
                adTypeAdditionalData = AdTypeAdditional.Rewarded(
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
                    .filterIsInstance<AdSource.Rewarded<*>>()
                    .first()
                    .show(activity)
            }
        }
    }

    override fun setRewardedListener(listener: RewardedListener) {
        logInfo(Tag, "Set rewarded listener")
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
        require(adSource is AdSource.Rewarded<*>)
        observeCallbacksJob = adSource.state.onEach { state ->
            when (state) {
                State.Initialized,
                is State.Bid.Failure,
                State.Bid.Requesting,
                is State.Bid.Success,
                is State.Fill.Failure,
                State.Fill.LoadingResources,
                is State.Fill.Success -> {
                    // do nothing
                }
                is State.Expired -> listener.onAdExpired(state.ad)
                is State.Show.Clicked -> listener.onAdClicked(state.ad)
                is State.Show.Closed -> listener.onAdClosed(state.ad)
                is State.Show.Impression -> listener.onAdImpression(state.ad)
                is State.Show.ShowFailed -> listener.onAdShowFailed(state.cause)
                is OnReward.Success -> listener.onUserRewarded(state.ad, state.reward)
            }
        }.launchIn(scope)
    }

    private fun getRewardedListener() = object : RewardedListener {
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

        override fun onUserRewarded(ad: Ad, reward: Reward?) {
            userListener?.onUserRewarded(ad, reward)
        }
    }

}

private const val Tag = "Rewarded"

