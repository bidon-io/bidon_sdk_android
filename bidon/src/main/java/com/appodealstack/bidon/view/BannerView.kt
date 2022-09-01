package com.appodealstack.bidon.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.R
import com.appodealstack.bidon.ad.BannerListener
import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdState
import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.CountDownTimer
import com.appodealstack.bidon.auctions.domain.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.view.helper.BannerState.*
import com.appodealstack.bidon.view.helper.wrapUserBannerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
) : FrameLayout(context, attrs, defStyleAtt), BannerAd {

    override var placementId: String = BidOnSdk.DefaultPlacement
    private var bannerSize: BannerSize = BannerSize.Banner
    private var userListener: BannerListener? = null
    private var refresh: AutoRefresh = AutoRefresh.On(DefaultAutoRefreshTimeoutMs)

    constructor(context: Context, placementId: String) : this(context, null, 0) {
        this.placementId = placementId
    }

    private val showState = MutableStateFlow<ShowState>(ShowState.Idle)
    private val showActionFlow = MutableSharedFlow<ShowAction>(extraBufferCapacity = Int.MAX_VALUE)
    private val loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    private val loadActionFlow = MutableSharedFlow<LoadAction>(extraBufferCapacity = Int.MAX_VALUE)

    private val pauseResumeObserver: PauseResumeObserver by lazy { get() }
    private val demandAd by lazy { DemandAd(AdType.Banner, placementId) }
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val displayingRefreshTimer by lazy { get<CountDownTimer>() }
    private val loadingRefreshTimer by lazy { get<CountDownTimer>() }
    private var showJob: Job? = null
    private var observeCallbacksJob: Job? = null

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
        launchLoadReducer()
        launchShowReducer()
    }

    override fun setAdSize(bannerSize: BannerSize) {
        logInfo(Tag, "BannerSize set: $bannerSize")
        this.bannerSize = bannerSize
    }

    override fun setBannerListener(listener: BannerListener) {
        logInfo(Tag, "Set banner listener")
        this.userListener = listener
    }

    override fun load() {
        logInfo(Tag, "Load with placement invoked: $placementId")
        sendAction(LoadAction.OnLoadInvoked)
    }

    override fun show() {
        logInfo(Tag, "Show with placement invoked: $placementId")
        sendAction(ShowAction.OnShowInvoked)
    }

    override fun startAutoRefresh(timeoutMs: Long) {
        logInfo(Tag, "Auto-refresh initialized with timeout $timeoutMs ms")
        refresh = AutoRefresh.On(timeoutMs)
        sendAction(ShowAction.OnStartAutoRefreshInvoked(timeoutMs))
    }

    override fun stopAutoRefresh() {
        logInfo(Tag, "Auto-refresh stopped")
        refresh = AutoRefresh.Off
        sendAction(ShowAction.OnStopAutoRefreshInvoked)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    override fun destroy() {
        sendAction(LoadAction.OnDestroyInvoked)
        sendAction(ShowAction.OnDestroyInvoked)
    }

    /**
     * Private
     */

    private fun sendAction(action: ShowAction) {
        if (!BidON.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        showActionFlow.tryEmit(action)
    }

    private fun sendAction(action: LoadAction) {
        if (!BidON.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        loadActionFlow.tryEmit(action)
    }

    private fun launchLoadReducer() {
        loadActionFlow.scan(
            initial = loadState.value,
            operation = { state, action ->
                logInternal(Tag, "Load Action: ${action.javaClass.simpleName}. Current State: ${state.javaClass.simpleName}.")
                when (action) {
                    LoadAction.OnLoadInvoked -> {
                        when (state) {
                            LoadState.Idle -> {
                                startAuction()
                                LoadState.Loading
                            }
                            LoadState.Loading -> {
                                logInternal(Tag, "Auction already in progress. Placement($placementId).")
                                state
                            }
                            is LoadState.Loaded -> {
                                logInternal(Tag, "Auction is completed and winner exists. Placement($placementId).")
                                state
                            }
                        }
                    }
                    is LoadAction.OnAuctionSucceed -> {
                        /**
                         * Winner found
                         */
                        val winner = action.auctionResults.first()
                        val ad = requireNotNull(winner.adSource.ad) {
                            "[Ad] should exist when an Action succeeds"
                        }
                        listener.auctionSucceed(action.auctionResults)
                        listener.onAdLoaded(ad)
                        LoadState.Loaded(winner)
                    }
                    is LoadAction.OnAuctionFailed -> {
                        /**
                         * Auction failed
                         */
                        listener.auctionFailed(error = action.cause)
                        listener.onAdLoadFailed(cause = action.cause)
                        launchLoadingRefreshIfNeeded()
                        LoadState.Idle
                    }
                    LoadAction.OnRefreshTimeoutFinished -> {
                        sendAction(LoadAction.OnLoadInvoked)
                        LoadState.Idle
                    }
                    LoadAction.OnWinnerTaken -> {
                        if (refresh is AutoRefresh.On) {
                            loadingRefreshTimer.stop()
                            sendAction(LoadAction.OnRefreshTimeoutFinished)
                        }
                        LoadState.Idle
                    }
                    LoadAction.OnDestroyInvoked -> {
                        loadingRefreshTimer.stop()
                        LoadState.Idle
                    }
                }
            }
        ).onEach {
            logInternal(Tag, "New Load state: ${it.javaClass.simpleName}")
            loadState.value = it
        }.launchIn(scope = scope)
    }

    private fun launchShowReducer() {
        showActionFlow.scan(
            initial = showState.value,
            operation = { state, action ->
                logInternal(Tag, "Show Action: ${action.javaClass.simpleName}. Current State: ${state.javaClass.simpleName}.")
                when (action) {
                    ShowAction.OnShowInvoked -> {
                        proceedShow()
                        state
                    }
                    is ShowAction.OnAdShown -> {
                        listener.onAdImpression(action.ad)
                        BidON.logRevenue(action.ad)
                        (state as? ShowState.Displaying)?.auctionResult?.adSource?.destroy()
                        launchDisplayingRefreshIfNeeded()
                        ShowState.Displaying(action.winner)
                    }
                    ShowAction.OnRefreshTimeoutFinished -> {
                        sendAction(ShowAction.OnShowInvoked)
                        state
                    }
                    is ShowAction.OnStartAutoRefreshInvoked -> {
                        if (showState.value is ShowState.Displaying) {
                            launchLoadingRefreshIfNeeded()
                            launchDisplayingRefreshIfNeeded()
                        }
                        state
                    }
                    ShowAction.OnStopAutoRefreshInvoked -> {
                        displayingRefreshTimer.stop()
                        loadingRefreshTimer.stop()
                        state
                    }
                    ShowAction.OnDestroyInvoked -> {
                        observeCallbacksJob?.cancel()
                        observeCallbacksJob = null
                        displayingRefreshTimer.stop()
                        this.removeAllViews()
                        ShowState.Idle
                    }
                }
            }
        ).onEach {
            logInternal(Tag, "New Show state: ${it.javaClass.simpleName}")
            showState.value = it
        }.launchIn(scope = scope)
    }

    private fun launchLoadingRefreshIfNeeded() {
        (refresh as? AutoRefresh.On)?.timeoutMs?.let { timeoutMs ->
            logInternal(Tag, "Launching Loading CountDownTimer: $timeoutMs ms")
            loadingRefreshTimer.startTimer(timeoutMs) {
                sendAction(LoadAction.OnRefreshTimeoutFinished)
            }
        }
    }

    private fun launchDisplayingRefreshIfNeeded() {
        (refresh as? AutoRefresh.On)?.timeoutMs?.let { timeoutMs ->
            logInternal(Tag, "Launching Display CountDownTimer: $timeoutMs ms")
            displayingRefreshTimer.startTimer(timeoutMs) {
                sendAction(ShowAction.OnRefreshTimeoutFinished)
            }
        }
    }

    private fun proceedShow() {
        showJob?.cancel()
        if (refresh is AutoRefresh.Off && loadState.value !is LoadState.Loaded) {
            logInternal(Tag, "AutoRefresh is OFF and no banner loaded. Unable to show banner.")
            return
        }
        showJob = scope.launch {
            try {
                val winner = (loadState.first { it is LoadState.Loaded } as? LoadState.Loaded)?.auctionResult ?: return@launch
                sendAction(LoadAction.OnWinnerTaken)
                subscribeToWinner(winner.adSource)
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
                sendAction(ShowAction.OnAdShown(winner, ad))
            } catch (e: Exception) {
            }
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
                    adContainer = this@BannerView
                ),
                roundsListener = listener
            ).onSuccess { auctionResults ->
                sendAction(LoadAction.OnAuctionSucceed(auctionResults))
            }.onFailure {
                sendAction(LoadAction.OnAuctionFailed(it))
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
}

private const val Tag = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L