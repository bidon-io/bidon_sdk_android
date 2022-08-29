package com.appodealstack.bidon.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.R
import com.appodealstack.bidon.ad.BannerListener
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.AuctionHolder
import com.appodealstack.bidon.auctions.domain.AutoRefresher
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

interface BannerAd {
    val placementId: String

    fun setAdSize(bannerSize: BannerSize)
    fun load()
    fun show()
    fun destroy()
    fun setBannerListener(listener: BannerListener)

    /**
     * By default AutoRefresh is on with [DefaultAutoRefreshTimeoutMs]
     */
    fun startAutoRefresh(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()

    interface AutoRefreshable {
        fun onRefresh()
    }
}

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0
) : FrameLayout(context, attrs, defStyleAtt), BannerAd, BannerAd.AutoRefreshable {

    constructor(context: Context, placementId: String) : this(context, null, 0) {
        this.placementId = placementId
    }

    override var placementId: String = DefaultPlacement

    private var bannerSize: BannerSize = BannerSize.Banner
    private val isBannerDisplaying = AtomicBoolean(false)

    private val demandAd by lazy {
        DemandAd(AdType.Banner, placementId)
    }

    private val autoRefresher: AutoRefresher by lazy {
        get {
            params(this@BannerView as BannerAd.AutoRefreshable)
        }
    }

    private val auctionHolder: AuctionHolder by lazy {
        get { params(demandAd to listener) }
    }
    private var userListener: BannerListener? = null
    private var observeCallbacksJob: Job? = null

    private val listener by lazy {
        getBannerListener()
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                getString(R.styleable.BannerView_placementId)?.let {
                    this@BannerView.placementId = it
                }
                getInteger(R.styleable.BannerView_bannerSize, 0).let {
                    when (it) {
                        1 -> setAdSize(BannerSize.Banner)
                        2 -> setAdSize(BannerSize.Large)
                        3 -> setAdSize(BannerSize.LeaderBoard)
                        4 -> setAdSize(BannerSize.MRec)
                        5 -> setAdSize(BannerSize.Adaptive)
                    }
                }
            } finally {
                recycle()
            }
        }
    }

    override fun setAdSize(bannerSize: BannerSize) {
        logInfo(Tag, "BannerSize set: $bannerSize")
        this.bannerSize = bannerSize
    }

    override fun load() {
        if (!BidON.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        logInfo(Tag, "Load with placement: $placementId")
        if (!auctionHolder.isActive) {
            listener.auctionStarted()
            auctionHolder.startAuction(
                adTypeAdditional = AdTypeAdditional.Banner(
                    bannerSize = bannerSize,
                    adContainer = this@BannerView
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
                            listener.onAdLoadFailed(cause = it)
                        }
                }
            )
        } else {
            logInfo(Tag, "Auction already in progress. Placement: $placementId.")
        }
    }

    override fun show() {
        removeAllViews()
        auctionHolder.popWinner()?.let { adSource ->
            val ad = requireNotNull(adSource.ad) {
                "Ad should exist on start of displaying [${adSource.demandId}]"
            }
            require(adSource is AdSource.Banner) {
                "Unexpected AdSource type. Expected: AdSource.Banner. Actual: ${adSource::class.java}."
            }
            addView(adSource.getAdView())
            isBannerDisplaying.set(true)
            listener.onAdImpression(ad)
        }
    }

    override fun setBannerListener(listener: BannerListener) {
        logInfo(Tag, "Set banner listener")
        this.userListener = listener
    }

    override fun startAutoRefresh(timeoutMs: Long) {
        logInfo(Tag, "Auto-refresh initialized with timeout $timeoutMs ms")
        autoRefresher.setAutoRefreshTimeout(timeoutMs)
        if (isBannerDisplaying.get()) {
            autoRefresher.launchRefresh()
        }
    }

    override fun stopAutoRefresh() {
        logInfo(Tag, "Auto-refresh stopped")
        autoRefresher.stopAutoRefresh()
    }

    override fun onRefresh() {
        load()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    override fun destroy() {
        auctionHolder.destroy()
        autoRefresher.stopAutoRefresh()
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
        isBannerDisplaying.set(false)
        this.removeAllViews()
    }

    /**
     * Private
     */

    private fun subscribeToWinner(adSource: AdSource<*>) {
        observeCallbacksJob?.cancel()
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
        }.launchIn(CoroutineScope(SdkDispatchers.Main))
    }

    private fun getBannerListener() = object : BannerListener {
        override fun onAdLoaded(ad: Ad) {
            userListener?.onAdLoaded(ad)
            autoRefresher.launchRefresh()
        }

        override fun onAdLoadFailed(cause: Throwable) {
            userListener?.onAdLoadFailed(cause)
            autoRefresher.launchRefresh()
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
            show()
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

private const val Tag = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L