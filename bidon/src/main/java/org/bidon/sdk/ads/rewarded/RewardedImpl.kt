package org.bidon.sdk.ads.rewarded

import android.app.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionHolder
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.impl.asBidonErrorOrUnspecified
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get

internal class RewardedImpl(
    dispatcher: CoroutineDispatcher = SdkDispatchers.Main,
    private val demandAd: DemandAd = DemandAd(AdType.Rewarded)
) : Rewarded, Extras by demandAd {

    private var userListener: RewardedListener? = null
    private var observeCallbacksJob: Job? = null
    private val auctionHolder: AuctionHolder by lazy {
        get { params(demandAd) }
    }
    private val listener by lazy {
        getRewardedListener()
    }
    private val scope by lazy {
        CoroutineScope(dispatcher)
    }

    override fun isReady(): Boolean {
        return auctionHolder.isAdReady()
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        if (auctionHolder.isAdReady()) {
            logInfo(TAG, "Ad is loaded and available to show.")
            return
        }
        logInfo(TAG, "Load (pricefloor=$pricefloor)")
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null

        if (!auctionHolder.isAuctionActive) {
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Rewarded(
                    activity = activity,
                    pricefloor = pricefloor,
                ),
                onResult = { result ->
                    result
                        .onSuccess { auctionResults ->
                            /**
                             * Winner found
                             */
                            val winner = auctionResults.first()
                            subscribeToWinner(winner.adSource)
                            listener.onAdLoaded(
                                requireNotNull(winner.adSource.ad) {
                                    "[Ad] should exist when the Action succeeds"
                                }
                            )
                        }.onFailure {
                            /**
                             * Auction failed
                             */
                            listener.onAdLoadFailed(cause = it.asBidonErrorOrUnspecified())
                        }
                }
            )
        } else {
            logInfo(TAG, "Auction already in progress")
        }
    }

    override fun showAd(activity: Activity) {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            listener.onAdShowFailed(BidonError.SdkNotInitialized)
            return
        }
        logInfo(TAG, "Show")
        if (auctionHolder.isAuctionActive) {
            logInfo(TAG, "Show failed. Auction in progress.")
            listener.onAdShowFailed(BidonError.AdNotReady)
            return
        }
        when (val adSource = auctionHolder.popWinnerForShow()) {
            null -> {
                logInfo(TAG, "Show failed. No Auction results.")
                listener.onAdShowFailed(BidonError.AdNotReady)
            }

            else -> {
                scope.launch(Dispatchers.Main.immediate) {
                    (adSource as AdSource.Rewarded).show(activity)
                }
            }
        }
    }

    override fun setRewardedListener(listener: RewardedListener) {
        logInfo(TAG, "Set rewarded listener")
        this.userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {
        auctionHolder.notifyLoss(
            winnerDemandId = winnerDemandId,
            winnerEcpm = winnerEcpm,
            onAuctionCancelled = {
                userListener?.onAdLoadFailed(BidonError.AuctionCancelled)
            },
            onNotified = {
                destroyAd()
            }
        )
    }

    override fun notifyWin() {
        auctionHolder.notifyWin()
    }

    override fun destroyAd() {
        scope.launch(Dispatchers.Main.immediate) {
            auctionHolder.destroy()
            observeCallbacksJob?.cancel()
            observeCallbacksJob = null
        }
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Rewarded<*>)
        observeCallbacksJob = adSource.adEvent.onEach { adEvent ->
            when (adEvent) {
                is AdEvent.Fill -> {
                    // do nothing
                }

                is AdEvent.OnReward -> {
                    listener.onUserRewarded(adEvent.ad, adEvent.reward)
                    adSource.sendRewardImpression()
                }

                is AdEvent.Clicked -> {
                    listener.onAdClicked(adEvent.ad)
                    adSource.sendClickImpression()
                }

                is AdEvent.Closed -> listener.onAdClosed(adEvent.ad)
                is AdEvent.Shown -> {
                    listener.onAdShown(adEvent.ad)
                    adSource.sendShowImpression()
                }

                is AdEvent.PaidRevenue -> listener.onRevenuePaid(adEvent.ad, adEvent.adValue)
                is AdEvent.ShowFailed -> listener.onAdShowFailed(adEvent.cause)
                is AdEvent.LoadFailed -> listener.onAdLoadFailed(adEvent.cause)
                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
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

        override fun onUserRewarded(ad: Ad, reward: Reward?) {
            userListener?.onUserRewarded(ad, reward)
        }

        override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
            userListener?.onRevenuePaid(ad, adValue)
        }
    }
}

private const val TAG = "Rewarded"
