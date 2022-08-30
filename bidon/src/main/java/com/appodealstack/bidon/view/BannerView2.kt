package com.appodealstack.bidon.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.R
import com.appodealstack.bidon.ad.BannerListener
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.CountDownTimer
import com.appodealstack.bidon.auctions.domain.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.view.Banner.Action
import com.appodealstack.bidon.view.Banner.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal interface Banner {
    sealed interface State {
        object Idle : State
        object Displaying : State
        object Loading : State

        class ReadyToShow(
            val winner: AuctionResult
        ) : State
    }

    sealed interface Action {
        class OnAuctionSucceed(val auctionResults: List<AuctionResult>) : Action
        class OnAuctionFailed(val cause: Throwable) : Action

        object OnLoadInvoked : Action
        object OnShowInvoked : Action
        object OnDestroyInvoked : Action

        class OnAdShown(val winner: AuctionResult, val ad: Ad) : Action

        class OnStartAutoRefreshInvoked(val timeoutMs: Long) : Action
        object OnStopAutoRefreshInvoked : Action
        object OnRefreshTimeoutFinished : Action
    }
}

class BannerView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0
) : FrameLayout(context, attrs, defStyleAtt), BannerAd {

    constructor(context: Context, placementId: String) : this(context, null, 0) {
        this.placementId = placementId
    }

    override var placementId: String = BidOnSdk.DefaultPlacement
    private var bannerSize: BannerSize = BannerSize.Banner
    private var userListener: BannerListener? = null

    @Suppress("MemberVisibilityCanBePrivate")
    internal val stateFlow = MutableStateFlow<State>(State.Idle)

    private val pauseResumeObserver: PauseResumeObserver by lazy { get() }
    private val actionFlow = MutableSharedFlow<Action>(extraBufferCapacity = Int.MAX_VALUE)
    private val demandAd by lazy { DemandAd(AdType.Banner, placementId) }
    private val listener by lazy { getBannerListener() }
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private var observeCallbacksJob: Job? = null

    private var refresh: AutoRefresh = AutoRefresh.On(DefaultAutoRefreshTimeoutMs)
    private val displayingRefresh by lazy { get<CountDownTimer>() }
    private val loadingRefresh by lazy { get<CountDownTimer>() }
    private var showJob: Job? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                getString(R.styleable.BannerView_placementId)?.let {
                    this@BannerView2.placementId = it
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
        launchReducer()
    }

    override fun setAdSize(bannerSize: BannerSize) {
        logInfo(Tag, "BannerSize set: $bannerSize")
        this.bannerSize = bannerSize
    }

    override fun load() {
        logInfo(Tag, "Load with placement invoked: $placementId")
        sendAction(Action.OnLoadInvoked)
    }

    override fun show() {
        logInfo(Tag, "Show with placement invoked: $placementId")
        sendAction(Action.OnShowInvoked)
    }

    override fun setBannerListener(listener: BannerListener) {
        logInfo(Tag, "Set banner listener")
        this.userListener = listener
    }

    override fun startAutoRefresh(timeoutMs: Long) {
        logInfo(Tag, "Auto-refresh initialized with timeout $timeoutMs ms")
        sendAction(Action.OnStartAutoRefreshInvoked(timeoutMs))
    }

    override fun stopAutoRefresh() {
        logInfo(Tag, "Auto-refresh stopped")
        sendAction(Action.OnStopAutoRefreshInvoked)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sendAction(Action.OnDestroyInvoked)
    }

    override fun destroy() {
        sendAction(Action.OnDestroyInvoked)
    }

    /**
     * Private
     */

    private fun sendAction(action: Action) {
        actionFlow.tryEmit(action)
    }

    private val displayingWinner = MutableStateFlow<AuctionResult?>(null)
    private val awaitingToDisplayWinner = MutableStateFlow<AuctionResult?>(null)

    private fun launchReducer() {
        var displayingResult: AuctionResult? = null
        var showOnLoadImmediately: Boolean = false

        actionFlow.scan(
            initial = stateFlow.value,
            operation = { state, action ->
                logInternal(Tag, "Action: ${action.javaClass.simpleName}. Current State: ${state.javaClass.simpleName}.")
                if (!BidON.isInitialized()) {
                    logInfo(Tag, "Sdk is not initialized")
                    return@scan state
                }
                when (action) {
                    Action.OnDestroyInvoked -> {
                        proceedStopAutoRefresh()
                        showJob?.cancel()
                        showJob = null
                        observeCallbacksJob?.cancel()
                        observeCallbacksJob = null
                        this.removeAllViews()
                        displayingResult?.adSource?.destroy()
                        displayingResult = null
                        State.Idle
                    }
                    Action.OnLoadInvoked -> {
                        when (state) {
                            State.Displaying,
                            State.Idle -> {
                                startAuction()
                                State.Loading
                            }
                            State.Loading -> {
                                logInternal(Tag, "Auction already in progress. Placement($placementId).")
                                state
                            }
                            is State.ReadyToShow -> {
                                logInternal(Tag, "Auction is completed and winner exists. Placement($placementId).")
                                state
                            }
                        }
                    }
                    Action.OnShowInvoked -> {
                        when (state) {
                            State.Idle,
                            State.Displaying,
                            State.Loading -> {
                                logInternal(Tag, "No Ad loaded. Placement($placementId). State: $state")
                            }
                            is State.ReadyToShow -> {
                                val winner = state.winner
                                subscribeToWinner(winner.adSource)
                                proceedShow(winner)
                            }
                        }
                        state
                    }
                    is Action.OnAuctionFailed -> {
                        /**
                         * Auction failed
                         */
                        listener.auctionFailed(error = action.cause)
                        listener.onAdLoadFailed(cause = action.cause)
                        launchLoadingRefreshIfNeeded()
                        state
                    }
                    is Action.OnAuctionSucceed -> {
                        /**
                         * Winner found
                         */
                        val winner = action.auctionResults.first()
                        listener.auctionSucceed(action.auctionResults)
                        listener.onAdLoaded(
                            requireNotNull(winner.adSource.ad) {
                                "[Ad] should exist when the Action succeeds"
                            }
                        )
                        if (displayingResult == null) {
                            sendAction(Action.OnShowInvoked)
                        }
                        State.ReadyToShow(winner)
                    }
                    Action.OnRefreshTimeoutFinished -> {
                        sendAction(Action.OnShowInvoked)
                        state
                    }
                    is Action.OnStartAutoRefreshInvoked -> {
                        setAutoRefreshTimeout(action.timeoutMs)
                        if (state is State.Displaying) {
                            launchDisplayingRefreshIfNeeded()
                        }
                        state
                    }
                    Action.OnStopAutoRefreshInvoked -> {
                        proceedStopAutoRefresh()
                        state
                    }
                    is Action.OnAdShown -> {
                        listener.onAdImpression(action.ad)
                        // destroy old AdView
                        displayingResult?.adSource?.destroy()
                        // save new AdView/AuctionResult
                        displayingResult = action.winner
                        sendAction(Action.OnLoadInvoked)
                        launchDisplayingRefreshIfNeeded()
                        State.Displaying
                    }
                }
            }
        ).onEach {
            logInternal(Tag, "New state: ${it.javaClass.simpleName}")
            stateFlow.value = it
        }.launchIn(scope = scope)
    }

    private fun setAutoRefreshTimeout(timeoutMs: Long) {
        refresh = AutoRefresh.On(timeoutMs)
        if (stateFlow.value is State.Displaying) {
            launchDisplayingRefreshIfNeeded()
        }
    }

    private fun launchDisplayingRefreshIfNeeded() {
        logInternal(Tag, "launchRefreshIfNeeded")
        (refresh as? AutoRefresh.On)?.let {
            displayingRefresh.startTimer(it.timeoutMs) {
                sendAction(Action.OnRefreshTimeoutFinished)
            }
        }
    }

    private fun launchLoadingRefreshIfNeeded() {
        logInternal(Tag, "launchRefreshIfNeeded")
        ((refresh as? AutoRefresh.On)?.timeoutMs ?: DefaultAutoRefreshTimeoutMs).let {
            loadingRefresh.startTimer(timeoutMs = it) {
                sendAction(Action.OnLoadInvoked)
                launchDisplayingRefreshIfNeeded()
            }
        }
    }

    private fun proceedStopAutoRefresh() {
        refresh = AutoRefresh.Off
        displayingRefresh.stop()
    }

    private fun proceedShow(winner: AuctionResult) {
        showJob?.cancel()
        showJob = scope.launch {
            val adSource = requireNotNull(winner.adSource as? AdSource.Banner) {
                "Unexpected AdSource type. Expected: AdSource.Banner. Actual: ${winner.adSource::class.java}."
            }
            val ad = requireNotNull(adSource.ad) {
                "Ad should exist on start of displaying [${adSource.demandId}]"
            }
            // wait if app in background
            pauseResumeObserver.lifecycleFlow.first {
                val isResumed = it == PauseResumeObserver.LifecycleState.Resumed
                if (!isResumed) {
                    logInternal(Tag, "Showing is waiting for Resumed state. Current: $it")
                }
                isResumed
            }
            // add AdView to Screen
            removeAllViews()
            addView(adSource.getAdView())
            sendAction(Action.OnAdShown(winner, ad))
        }
    }

    private fun startAuction() {
        listener.auctionStarted()
        scope.launch {
            get<Auction>().start(
                demandAd = demandAd,
                resolver = MaxEcpmAuctionResolver,
                adTypeAdditionalData = AdTypeAdditional.Banner(
                    bannerSize = bannerSize,
                    adContainer = this@BannerView2
                ),
                roundsListener = listener
            ).onSuccess { auctionResults ->
                sendAction(Action.OnAuctionSucceed(auctionResults))
            }.onFailure {
                sendAction(Action.OnAuctionFailed(it))
            }
        }
    }

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

private const val Tag = "BannerView2"