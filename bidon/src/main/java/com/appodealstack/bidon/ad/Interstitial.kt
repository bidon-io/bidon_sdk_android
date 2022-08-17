package com.appodealstack.bidon.ad

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.NewAuction
import com.appodealstack.bidon.auctions.domain.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Interstitial(
    override val placementId: String = DefaultPlacement
) : InterstitialAd by InterstitialAdImpl(placementId)

interface InterstitialAd {
    val placementId: String

    fun load(placement: String? = null)
    fun show(activity: Activity, placement: String? = null)
    fun setInterstitialListener(listener: InterstitialListener)
}

internal class InterstitialAdImpl(
    override val placementId: String,
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Default,
) : InterstitialAd {

    private val demandAd by lazy {
        DemandAd(AdType.Interstitial, placementId)
    }
    private val auction: NewAuction get() = get()
    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private var userListener: InterstitialListener? = null
    private val auctionResults = mutableListOf<AuctionResult>()
    private var auctionJob: Job? = null

    private val listener by lazy {
        getInterstitialListener()
    }

    override fun load(placement: String?) {
        logInfo(Tag, "Load with placement: $placement")
        if (auctionJob?.isActive == true) return
        auctionJob = scope.launch {
            auctionResults.clear()
            listener.auctionStarted()
            auction.start(
                demandAd = demandAd,
                resolver = MaxEcpmAuctionResolver,
                adTypeAdditionalData = AdTypeAdditional.Interstitial(
                    activity = null
                ),
                roundsListener = listener
            ).onSuccess { results ->
                logInfo(Tag, "Auction completed successfully: $results")
                auctionResults.addAll(results)
                listener.auctionSucceed(results)
                // listener.onAdLoaded(results.first().adSource.ad)
            }.onFailure {
                logError(Tag, "Auction failed", it)
                auctionResults.clear()
                listener.onAdLoadFailed(cause = it)
            }
        }
    }

    override fun show(activity: Activity, placement: String?) {
        logInfo(Tag, "Show with placement: $placement")
        if (auctionJob?.isActive != true) {
            auctionResults.firstOrNull()?.let {
                when (val adSource = it.adSource) {
                    is AdSource.Interstitial<*> -> adSource.show(activity)
                }
            }
        }
    }

    override fun setInterstitialListener(listener: InterstitialListener) {
        logInfo(Tag, "Set interstitial listener")
        this.userListener = listener
    }

    /**
     * Private
     */
    private fun getInterstitialListener() = object : InterstitialListener {
        override fun onAdLoaded(ad: Ad) {
            userListener?.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: Throwable) {
            userListener?.onAdLoadFailed(cause)
        }

        override fun onAdShown(ad: Ad) {
            userListener?.onAdShown(ad)
        }

        override fun onAdShowFailed(cause: Throwable) {
            userListener?.onAdShowFailed(cause)
        }

        override fun onAdImpression(ad: Ad) {
            userListener?.onAdImpression(ad)
        }

        override fun onAdClicked(ad: Ad) {
            userListener?.onAdClicked(ad)
        }

        override fun onAdClosed(ad: Ad) {
            userListener?.onAdClicked(ad)
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
}

private const val Tag = "Interstitial"

