package org.bidon.sdk.ads.interstitial

import android.app.Activity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.asUnspecified
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionHolder
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get

internal class InterstitialImpl(
    dispatcher: CoroutineDispatcher = SdkDispatchers.Main,
    private val demandAd: DemandAd = DemandAd(AdType.Interstitial)
) : Interstitial, Extras by demandAd {
    private var userListener: InterstitialListener? = null
    private var observeCallbacksJob: Job? = null
    private val auctionHolder: AuctionHolder by lazy {
        get {
            params(demandAd)
        }
    }
    private val listener by lazy {
        getInterstitialListener()
    }
    private val scope by lazy {
        CoroutineScope(dispatcher)
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        if (auctionHolder.isAdReady()) {
            logInfo(Tag, "Ad is loaded and available to show.")
            return
        }
        logInfo(Tag, "Load (pricefloor=$pricefloor)")
        if (!auctionHolder.isActive) {
            auctionHolder.startAuction(
                adTypeParam = AdTypeParam.Interstitial(
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
                                    "[Ad] should exist when action succeeds"
                                }
                            )
                        }.onFailure {
                            /**
                             * Auction failed
                             */
                            listener.onAdLoadFailed(cause = it.asUnspecified())
                        }
                }
            )
        } else {
            logInfo(Tag, "Auction already in progress")
        }
    }

    override fun showAd(activity: Activity) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            listener.onAdShowFailed(BidonError.SdkNotInitialized)
            return
        }
        logInfo(Tag, "Show")
        if (auctionHolder.isActive) {
            logInfo(Tag, "Show failed. Auction in progress.")
            listener.onAdShowFailed(BidonError.AuctionInProgress)
            return
        }
        when (val adSource = auctionHolder.popWinner()) {
            null -> {
                logInfo(Tag, "Show failed. No Auction results.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
            else -> {
                scope.launch(Dispatchers.Main.immediate) {
                    adSource.show(activity)
                }
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(Tag, "Set interstitial listener")
        this.userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {
        logInfo(Tag, "Notify Loss invoked with Winner($winnerDemandId, $winnerEcpm)")
        auctionHolder.popWinner()?.sendLoss(winnerDemandId, winnerEcpm, StatisticsCollector.AdType.Interstitial)
        destroyAd()
    }

    override fun destroyAd() {
        scope.launch(Dispatchers.Main.immediate) {
            auctionHolder.destroy()
            observeCallbacksJob?.cancel()
            observeCallbacksJob = null
        }
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
                    listener.onAdClicked(adEvent.ad)
                    adSource.sendClickImpression(adType = StatisticsCollector.AdType.Interstitial)
                }
                is AdEvent.Closed -> listener.onAdClosed(adEvent.ad)
                is AdEvent.Shown -> {
                    listener.onAdShown(adEvent.ad)
                    adSource.sendShowImpression(adType = StatisticsCollector.AdType.Interstitial)
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

        override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
            userListener?.onRevenuePaid(ad, adValue)
        }
    }
}

private const val Tag = "Interstitial"
