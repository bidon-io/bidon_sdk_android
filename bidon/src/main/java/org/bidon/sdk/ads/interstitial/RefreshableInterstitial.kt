package org.bidon.sdk.ads.interstitial

import android.app.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.Cacheable
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

class RefreshableInterstitialAd(
    dispatcher: CoroutineDispatcher = SdkDispatchers.Main,
    private val demandAd: DemandAd = DemandAd(AdType.Interstitial),
    private val scope: CoroutineScope = CoroutineScope(dispatcher),
) : Interstitial, Cacheable, Extras by demandAd {
    private val tag get() = TAG
    private var userListener: InterstitialListener? = null
    private var observeCallbacksJob: Job? = null

    private val adCache: AdCache by lazy {
        get {
            params(demandAd)
        }
    }
    private val listener by lazy {
        getInterstitialListener()
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(tag, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        adCache.cache(
            adTypeParam = AdTypeParam.Interstitial(
                activity = activity,
                pricefloor = pricefloor,
            ),
            onEach = { auctionResult ->
                logInfo(tag, "Ad is loaded and available to show.")
                auctionResult.adSource.ad?.let { listener.onAdLoaded(it) }
            }
        )
    }

    override fun setCacheCapacity(capacity: Int) {
        adCache.setCacheCapacity(capacity)
    }

    override fun setMinCacheSize(minSize: Int) {
        adCache.setMinCacheSize(minSize)
    }

    override fun showAd(activity: Activity) {
        if (!BidonSdk.isInitialized()) {
            logInfo(tag, "Sdk is not initialized")
            listener.onAdShowFailed(BidonError.SdkNotInitialized)
            return
        }
        logInfo(tag, "Show")
        scope.launch {
            withTimeoutOrNull(500) {
                val winner = adCache.poll()
                logInfo(tag, "Ad is loaded and available to show.")
                subscribeToWinner(winner.adSource)
                (winner.adSource as AdSource.Interstitial).show(activity)
            } ?: run {
                logInfo(tag, "Ad is not loaded yet.")
                listener.onAdShowFailed(BidonError.FullscreenAdNotReady)
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(tag, "Set interstitial listener")
        this.userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {}
    override fun notifyWin() {}
    override fun destroyAd() {}

    override fun isReady(): Boolean {
        return adCache.peek()?.adSource?.isAdReadyToShow == true
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        require(adSource is AdSource.Interstitial<*>)
        observeCallbacksJob = adSource.adEvent.onEach { adEvent ->
            when (adEvent) {
                is AdEvent.OnReward,
                is AdEvent.Fill -> {
                    // do nothing
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

private const val MIN_CACHE_SIZE = 1
private const val CACHE_CAPACITY = 3