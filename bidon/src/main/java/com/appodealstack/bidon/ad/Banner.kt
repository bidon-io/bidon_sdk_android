package com.appodealstack.bidon.ad

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface BannerAd {
    val placementId: String

    fun setAdSize(bannerSize: BannerSize)
    fun load()
    fun destroy()
    fun setBannerListener(listener: BannerListener)

    /**
     * By default AutoRefresh is on with [DefaultAutoRefreshTimeoutMs]
     */
    fun startAutoRefresh(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()
}

class Banner private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0
) : BannerAd, FrameLayout(context, attrs, defStyleAtt) {

    constructor(
        context: Context,
        placementId: String,
    ) : this(context, null, 0) {
        this.placementId = placementId
    }

    override var placementId: String = DefaultPlacement

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private val demandAd by lazy {
        DemandAd(AdType.Banner, placementId)
    }
    private var auction: Auction? = null
    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private var userListener: BannerListener? = null
    private var auctionJob: Job? = null
    private var observeCallbacksJob: Job? = null

    private val listener by lazy {
        getBannerListener()
    }

    private var bannerSize: BannerSize = BannerSize.Banner

    override fun setAdSize(bannerSize: BannerSize) {
        logInfo(Tag, "BannerSize set: $bannerSize")
        this.bannerSize = bannerSize
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
                adTypeAdditionalData = AdTypeAdditional.Banner(
                    bannerSize = bannerSize,
                    adContainer = this@Banner
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
                val adContainer = this@Banner
                adContainer.removeAllViews()
                results.first().let { auctionResult ->
                    require(auctionResult.adSource is AdSource.Banner)
                    adContainer.addView(auctionResult.adSource.getAdView())
                }
            }.onFailure {
                logError(Tag, "Auction failed", it)
                listener.auctionFailed(error = it)
                listener.onAdLoadFailed(cause = it)
            }
        }
    }

    override fun setBannerListener(listener: BannerListener) {
        logInfo(Tag, "Set banner listener")
        this.userListener = listener
    }

    override fun startAutoRefresh(timeoutMs: Long) {
        logInfo(Tag, "Auto-refresh started with timeout $timeoutMs ms")
        TODO("Not implemented")
    }

    override fun stopAutoRefresh() {
        logInfo(Tag, "Auto-refresh stopped")
        TODO("Not implemented")
    }

    override fun destroy() {
        auctionJob?.cancel()
        auctionJob = null
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
        auction?.destroy()
        auction = null
        this.removeAllViews()
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        observeCallbacksJob = adSource.adState.onEach { state ->
            logInternal(Tag, "$state")
            when (state) {
                is AdState.Bid,
                is AdState.OnReward,
                is AdState.Fill -> {
                    // do nothing
                }
                is AdState.Clicked -> listener.onAdClicked(state.ad)
                is AdState.Closed -> listener.onAdClosed(state.ad)
                is AdState.Impression -> listener.onAdImpression(state.ad)
                is AdState.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdState.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdState.Expired -> listener.onAdExpired(state.ad)
            }
        }.launchIn(scope)
    }

    private fun getBannerListener() = object : BannerListener {
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
    }
}

private const val Tag = "Banner"
private const val DefaultAutoRefreshTimeoutMs = 15_000L