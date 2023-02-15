package com.appodealstack.bidon.ads.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultMinPrice
import com.appodealstack.bidon.R
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.AdState
import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.bidon.ads.asUnspecified
import com.appodealstack.bidon.ads.banner.helper.ActivityLifecycleState
import com.appodealstack.bidon.ads.banner.helper.BannerState.*
import com.appodealstack.bidon.ads.banner.helper.CountDownTimer
import com.appodealstack.bidon.ads.banner.helper.impl.ActivityLifecycleObserver
import com.appodealstack.bidon.ads.banner.helper.wrapUserBannerListener
import com.appodealstack.bidon.auction.AdTypeParam
import com.appodealstack.bidon.auction.Auction
import com.appodealstack.bidon.auction.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.auction.models.BannerRequestBody.Format
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.stats.StatisticsCollector.AdType.Banner
import com.appodealstack.bidon.utils.SdkDispatchers
import com.appodealstack.bidon.utils.di.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface BannerAd {
    val placementId: String

    fun setAdSize(bannerSize: BannerSize)
    fun load(minPrice: Double = DefaultMinPrice)
    fun show()
    fun destroy()
    fun setBannerListener(listener: BannerListener)

    /**
     * By default AutoRefresh is on with [DefaultAutoRefreshTimeoutMs]
     */
    fun startAutoRefresh(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()
}

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0
) : FrameLayout(context, attrs, defStyleAtt), BannerAd {

    override var placementId: String = BidOnSdk.DefaultPlacement
    private var bannerSize: BannerSize = BannerSize.Banner
    private var userListener: BannerListener? = null
    private val activityLifecycleObserver by lazy { (context as? Activity)?.let { ActivityLifecycleObserver(it) } }
    private var refresh: AutoRefresh = if (activityLifecycleObserver == null) {
        AutoRefresh.Off
    } else {
        AutoRefresh.On(DefaultAutoRefreshTimeoutMs)
    }

    constructor(context: Context, placementId: String) : this(context, null, 0) {
        this.placementId = placementId
    }

    private val showState = MutableStateFlow<ShowState>(ShowState.Idle)
    private val showActionFlow = MutableSharedFlow<ShowAction>(extraBufferCapacity = Int.MAX_VALUE)
    private val loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    private val loadActionFlow = MutableSharedFlow<LoadAction>(extraBufferCapacity = Int.MAX_VALUE)

    private val demandAd by lazy { DemandAd(AdType.Banner, placementId) }
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val displayingRefreshTimer by lazy {
        activityLifecycleObserver?.let { observer ->
            get<CountDownTimer> {
                params(observer)
            }
        }
    }
    private val loadingRefreshTimer by lazy {
        activityLifecycleObserver?.let { observer ->
            get<CountDownTimer> {
                params(observer)
            }
        }
    }
    private var showJob: Job? = null
    private var observeCallbacksJob: Job? = null
    private var minPrice: Double = DefaultMinPrice

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                getString(R.styleable.BannerView_placementId)?.let {
                    this@BannerView.placementId = it
                }
                getInteger(R.styleable.BannerView_bannerSize, 0).let {
                    when (it) {
                        1 -> setAdSize(BannerSize.Banner)
                        3 -> setAdSize(BannerSize.LeaderBoard)
                        4 -> setAdSize(BannerSize.MRec)
                        5 -> setAdSize(BannerSize.Adaptive)
                    }
                }
            } finally {
                recycle()
            }
        }
        launchLoadReducer(minPrice)
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

    override fun load(minPrice: Double) {
        this.minPrice = minPrice
        logInfo(Tag, "Load with placement invoked: $placementId")
        sendAction(LoadAction.OnLoadInvoked)
    }

    override fun show() {
        logInfo(Tag, "Show with placement invoked: $placementId")
        sendAction(ShowAction.OnShowInvoked)
    }

    override fun startAutoRefresh(timeoutMs: Long) {
        if (activityLifecycleObserver == null) {
            logInfo(
                Tag,
                "Auto-refresh is disabled, because BannerView created not with Activity context."
            )
            refresh = AutoRefresh.Off
            return
        }
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
        if (!BidOn.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        showActionFlow.tryEmit(action)
    }

    private fun sendAction(action: LoadAction) {
        if (!BidOn.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        loadActionFlow.tryEmit(action)
    }

    private fun launchLoadReducer(minPrice: Double) {
        loadActionFlow.scan(
            initial = loadState.value,
            operation = { state, action ->
                logInfo(
                    Tag,
                    "Load Action: ${action.javaClass.simpleName}. Current State: ${state.javaClass.simpleName}."
                )
                when (action) {
                    LoadAction.OnLoadInvoked -> {
                        when (state) {
                            LoadState.Idle -> {
                                startAuction(minPrice)
                                LoadState.Loading
                            }
                            LoadState.Loading -> {
                                logInfo(
                                    Tag,
                                    "Auction already in progress. Placement($placementId)."
                                )
                                state
                            }
                            is LoadState.Loaded -> {
                                logInfo(
                                    Tag,
                                    "Auction is completed and winner exists. Placement($placementId)."
                                )
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
                            loadingRefreshTimer?.stop()
                            sendAction(LoadAction.OnRefreshTimeoutFinished)
                        }
                        LoadState.Idle
                    }
                    LoadAction.OnDestroyInvoked -> {
                        loadingRefreshTimer?.stop()
                        LoadState.Idle
                    }
                }
            }
        ).onEach {
            logInfo(Tag, "New Load state: ${it.javaClass.simpleName}")
            loadState.value = it
        }.launchIn(scope = scope)
    }

    private fun launchShowReducer() {
        showActionFlow.scan(
            initial = showState.value,
            operation = { state, action ->
                logInfo(
                    Tag,
                    "Show Action: ${action.javaClass.simpleName}. Current State: ${state.javaClass.simpleName}."
                )
                when (action) {
                    ShowAction.OnShowInvoked -> {
                        proceedShow()
                        state
                    }
                    is ShowAction.OnAdShown -> {
                        listener.onAdShown(action.ad)
                        sendStatsShownAsync(action.winner.adSource)
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
                        displayingRefreshTimer?.stop()
                        loadingRefreshTimer?.stop()
                        state
                    }
                    ShowAction.OnDestroyInvoked -> {
                        observeCallbacksJob?.cancel()
                        observeCallbacksJob = null
                        displayingRefreshTimer?.stop()
                        this.removeAllViews()
                        ShowState.Idle
                    }
                }
            }
        ).onEach {
            logInfo(Tag, "New Show state: ${it.javaClass.simpleName}")
            showState.value = it
        }.launchIn(scope = scope)
    }

    private fun launchLoadingRefreshIfNeeded() {
        (refresh as? AutoRefresh.On)?.timeoutMs?.let { timeoutMs ->
            logInfo(Tag, "Launching Loading CountDownTimer: $timeoutMs ms")
            loadingRefreshTimer?.startTimer(timeoutMs) {
                sendAction(LoadAction.OnRefreshTimeoutFinished)
            }
        }
    }

    private fun launchDisplayingRefreshIfNeeded() {
        (refresh as? AutoRefresh.On)?.timeoutMs?.let { timeoutMs ->
            logInfo(Tag, "Launching Display CountDownTimer: $timeoutMs ms")
            displayingRefreshTimer?.startTimer(timeoutMs) {
                sendAction(ShowAction.OnRefreshTimeoutFinished)
            }
        }
    }

    private fun proceedShow() {
        showJob?.cancel()
        if (refresh is AutoRefresh.Off && loadState.value !is LoadState.Loaded) {
            logInfo(
                Tag,
                "AutoRefresh is OFF and no banner loaded. Unable to show banner."
            )
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
                activityLifecycleObserver?.lifecycleFlow?.first {
                    val isResumed = it == ActivityLifecycleState.Resumed
                    if (!isResumed) {
                        logInfo(
                            Tag,
                            "Showing is waiting for Activity Resumed state. Current: $it"
                        )
                    }
                    isResumed
                }
                // add AdView to Screen
                removeAllViews()

                val adViewHolder = adSource.getAdView()
                val layoutParams = LayoutParams(adViewHolder.widthPx, adViewHolder.heightPx).apply {
                    gravity = Gravity.CENTER
                }
                addView(adViewHolder.networkAdview, layoutParams)

                sendAction(ShowAction.OnAdShown(winner, ad))
            } catch (e: Exception) {
                logError(Tag, "Error while displaying BannerView", e)
            }
        }
    }

    private fun startAuction(
        minPrice: Double
    ) {
        listener.auctionStarted()
        scope.launch {
            get<Auction>().start(
                demandAd = demandAd,
                resolver = MaxEcpmAuctionResolver,
                adTypeParamData = AdTypeParam.Banner(
                    bannerSize = bannerSize,
                    adContainer = this@BannerView,
                    priceFloor = minPrice
                ),
                roundsListener = listener
            ).onSuccess { auctionResults ->
                sendAction(LoadAction.OnAuctionSucceed(auctionResults))
            }.onFailure {
                sendAction(LoadAction.OnAuctionFailed(it.asUnspecified()))
            }
        }
    }

    private fun subscribeToWinner(adSource: AdSource<*>) {
        observeCallbacksJob?.cancel()
        observeCallbacksJob = adSource.adState.onEach { state ->
            logInfo(Tag, "$state")
            when (state) {
                is AdState.Bid,
                is AdState.OnReward,
                is AdState.Fill -> {
                    // do nothing
                }
                is AdState.Clicked -> {
                    sendStatsClickedAsync(adSource)
                    listener.onAdClicked(state.ad)
                }
                is AdState.Closed -> listener.onAdClosed(state.ad)
                is AdState.Impression -> {
                    listener.onAdShown(state.ad)
                }
                is AdState.PaidRevenue -> listener.onRevenuePaid(state.ad)
                is AdState.ShowFailed -> listener.onAdLoadFailed(state.cause)
                is AdState.LoadFailed -> listener.onAdShowFailed(state.cause)
                is AdState.Expired -> listener.onAdExpired(state.ad)
            }
        }.launchIn(scope)
    }

    private suspend fun sendStatsClickedAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendClickImpression(
                    Banner(format = bannerSize.asBannerFormat())
                )
            }
        }
    }

    private suspend fun sendStatsShownAsync(adSource: AdSource<*>) {
        coroutineScope {
            launch {
                (adSource as? StatisticsCollector)?.sendShowImpression(
                    Banner(format = bannerSize.asBannerFormat())
                )
            }
        }
    }

    private fun BannerSize.asBannerFormat() = when (this) {
        BannerSize.Banner -> Format.Banner320x50
        BannerSize.LeaderBoard -> Format.LeaderBoard728x90
        BannerSize.MRec -> Format.MRec300x250
        BannerSize.Adaptive -> Format.AdaptiveBanner320x50
    }
}

private const val Tag = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L