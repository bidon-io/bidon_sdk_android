package org.bidon.sdk.ads.interstitial

import android.app.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.adapter.ext.notifyExternalLoss
import org.bidon.sdk.adapter.ext.notifyExternalWin
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.InitAwaiter
import org.bidon.sdk.ads.InitAwaiterImpl
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.impl.asBidonErrorOrUnspecified
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG

internal class InterstitialImpl(
    dispatcher: CoroutineDispatcher = SdkDispatchers.Main,
    private val auctionKey: String? = null,
    private val demandAd: DemandAd = DemandAd(AdType.Interstitial)
) : InitAwaiter by InitAwaiterImpl(),
    Interstitial,
    Extras by demandAd {
    private var userListener: InterstitialListener? = null
    private var winner: AdSource.Interstitial<*>? = null
    private var observeCallbacksJob: Job? = null

    private val adCache: AdCache by lazy {
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

    override fun isReady(): Boolean {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return false
        }
        return adCache.peek()?.adSource?.isAdReadyToShow == true
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        scope.launch(Dispatchers.Default) {
            initWaitAndContinueIfRequired(
                onSuccess = {
                    logInfo(TAG, "Load (pricefloor=$pricefloor)")
                    adCache.cache(
                        adTypeParam = AdTypeParam.Interstitial(
                            activity = activity,
                            pricefloor = pricefloor,
                            auctionKey = auctionKey,
                        ),
                        onSuccess = { auctionResult, auctionInfo ->
                            subscribeToWinner(auctionInfo, auctionResult.adSource)
                            listener.onAdLoaded(
                                ad = requireNotNull(auctionResult.adSource.ad) {
                                    "[Ad] should exist when action succeeds"
                                },
                                auctionInfo = auctionInfo
                            )
                        },
                        onFailure = { auctionResult, cause ->
                            listener.onAdLoadFailed(
                                auctionInfo = auctionResult,
                                cause = cause.asBidonErrorOrUnspecified()
                            )
                        }
                    )
                },
                onFailure = {
                    withContext(Dispatchers.Main) {
                        logInfo(TAG, "Sdk was initialized with error")
                        listener.onAdLoadFailed(
                            auctionInfo = null,
                            cause = BidonError.SdkNotInitialized
                        )
                    }
                }
            )
        }
    }

    override fun showAd(activity: Activity) {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            listener.onAdShowFailed(BidonError.SdkNotInitialized)
            return
        }
        logInfo(TAG, "Show")
        activity.runOnUiThread {
            val adSource = adCache.pop()?.adSource as? AdSource.Interstitial
            if (adSource == null) {
                logInfo(TAG, "Show failed. No Auction results.")
                listener.onAdShowFailed(BidonError.AdNotReady)
            } else {
                winner = adSource
                adSource.show(activity)
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(TAG, "Set interstitial listener")
        this.userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        adCache.pop()?.adSource?.notifyExternalLoss(winnerDemandId, winnerPrice)
        destroyAd()
    }

    override fun notifyWin() {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        val notifiedSource = winner ?: adCache.peek()?.adSource as? AdSource.Interstitial
        notifiedSource?.notifyExternalWin()
        winner = null
    }

    override fun destroyAd() {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            adCache.clear()
            observeCallbacksJob?.cancel()
            observeCallbacksJob = null
            winner = null
        }
    }

    /**
     * Private
     */

    private fun subscribeToWinner(auctionInfo: AuctionInfo, adSource: AdSource<*>) {
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

                is AdEvent.Closed -> {
                    listener.onAdClosed(adEvent.ad)
                    observeCallbacksJob?.cancel()
                    observeCallbacksJob = null
                }

                is AdEvent.Shown -> {
                    listener.onAdShown(adEvent.ad)
                    adSource.sendShowImpression()
                }

                is AdEvent.PaidRevenue -> listener.onRevenuePaid(adEvent.ad, adEvent.adValue)
                is AdEvent.ShowFailed -> listener.onAdShowFailed(adEvent.cause)
                is AdEvent.LoadFailed -> listener.onAdLoadFailed(auctionInfo, adEvent.cause)
                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
    }

    private fun getInterstitialListener() = object : InterstitialListener {
        override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
            userListener?.onAdLoaded(ad, auctionInfo)
        }

        override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
            userListener?.onAdLoadFailed(auctionInfo, cause)
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

private const val TAG = "Interstitial"
