package org.bidon.sdk.ads.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.BidonSdk.DefaultPricefloor
import org.bidon.sdk.R
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.asUnspecified
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.BannerState.*
import org.bidon.sdk.ads.banner.helper.CountDownTimer
import org.bidon.sdk.ads.banner.helper.impl.ActivityLifecycleObserver
import org.bidon.sdk.ads.banner.helper.wrapUserBannerListener
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get

/**
 * Created by Bidon Team on 06/02/2023.
 */
@Deprecated("Use Banner")
class BannerView(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0
) : FrameLayout(context, attrs, defStyleAtt), BannerAd {

    private var bannerFormat: BannerFormat = BannerFormat.Banner
    private var userListener: BannerListener? = null
    private val activityLifecycleObserver by lazy { (context as? Activity)?.let { ActivityLifecycleObserver(it) } }
    private var refresh: AutoRefresh = if (activityLifecycleObserver == null) {
        AutoRefresh.Off
    } else {
        AutoRefresh.On(DefaultAutoRefreshTimeoutMs)
    }

    private val showState = MutableStateFlow<ShowState>(ShowState.Idle)
    private val showActionFlow = MutableSharedFlow<ShowAction>(extraBufferCapacity = Int.MAX_VALUE)
    private val loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    private val loadActionFlow = MutableSharedFlow<LoadAction>(extraBufferCapacity = Int.MAX_VALUE)

    private val demandAd by lazy { DemandAd(AdType.Banner) }
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
    private var pricefloor: Double = DefaultPricefloor

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.BannerView, 0, 0).apply {
            try {
                getInteger(R.styleable.BannerView_bannerSize, 0).let {
                    when (it) {
                        1 -> setBannerFormat(BannerFormat.Banner)
                        3 -> setBannerFormat(BannerFormat.LeaderBoard)
                        4 -> setBannerFormat(BannerFormat.MRec)
                        5 -> setBannerFormat(BannerFormat.Adaptive)
                    }
                }
            } finally {
                recycle()
            }
        }
        launchLoadReducer(pricefloor)
        launchShowReducer()
    }

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        logInfo(Tag, "BannerSize set: $bannerFormat")
        this.bannerFormat = bannerFormat
    }

    override fun setBannerListener(listener: BannerListener) {
        logInfo(Tag, "Set banner listener")
        this.userListener = listener
    }

    override fun isReady(): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadAd(pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        this.pricefloor = pricefloor
        logInfo(Tag, "Load")
        sendAction(LoadAction.OnLoadInvoked)
    }

    override fun showAd() {
        logInfo(Tag, "Show")
        sendAction(ShowAction.OnShowInvoked)
    }

//    override fun startAutoRefresh(timeoutMs: Long) {
//        if (activityLifecycleObserver == null) {
//            logInfo(
//                Tag,
//                "Auto-refresh is disabled, because BannerView created not with Activity context."
//            )
//            refresh = AutoRefresh.Off
//            return
//        }
//        logInfo(Tag, "Auto-refresh initialized with timeout $timeoutMs ms")
//        refresh = AutoRefresh.On(timeoutMs)
//        sendAction(ShowAction.OnStartAutoRefreshInvoked(timeoutMs))
//    }
//
//    override fun stopAutoRefresh() {
//        logInfo(Tag, "Auto-refresh stopped")
//        refresh = AutoRefresh.Off
//        sendAction(ShowAction.OnStopAutoRefreshInvoked)
//    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroyAd()
    }

    override fun destroyAd() {
        sendAction(LoadAction.OnDestroyInvoked)
        sendAction(ShowAction.OnDestroyInvoked)
    }

    /**
     * Private
     */

    private fun sendAction(action: ShowAction) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        showActionFlow.tryEmit(action)
    }

    private fun sendAction(action: LoadAction) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            return
        }
        loadActionFlow.tryEmit(action)
    }

    private fun launchLoadReducer(pricefloor: Double) {
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
                                startAuction(pricefloor)
                                LoadState.Loading
                            }
                            LoadState.Loading -> {
                                logInfo(
                                    Tag,
                                    "Auction already in progress"
                                )
                                state
                            }
                            is LoadState.Loaded -> {
                                logInfo(
                                    Tag,
                                    "Auction is completed and winner exists"
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
                        listener.onAdLoaded(ad)
                        LoadState.Loaded(winner)
                    }
                    is LoadAction.OnAuctionFailed -> {
                        /**
                         * Auction failed
                         */
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
                        logInfo(Tag, "Showing is waiting for Activity Resumed state. Current: $it")
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
        pricefloor: Double
    ) {
        scope.launch {
            get<Auction>().start(
                demandAd = demandAd,
                resolver = MaxEcpmAuctionResolver,
                adTypeParamData = AdTypeParam.Banner(
                    bannerFormat = bannerFormat,
                    adContainer = this@BannerView,
                    pricefloor = pricefloor,
                ),
            ).onSuccess { auctionResults ->
                sendAction(LoadAction.OnAuctionSucceed(auctionResults))
            }.onFailure {
                sendAction(LoadAction.OnAuctionFailed(it.asUnspecified()))
            }
        }
    }

    private fun subscribeToWinner(adSource: AdSource<*>) {
        observeCallbacksJob?.cancel()
        observeCallbacksJob = adSource.adEvent.onEach { adEvent ->
            logInfo(Tag, "$adEvent")
            when (adEvent) {
                is AdEvent.Bid,
                is AdEvent.OnReward,
                is AdEvent.Closed,
                is AdEvent.LoadFailed,
                is AdEvent.Fill -> {
                    // do nothing
                }
                is AdEvent.Clicked -> {
                    listener.onAdClicked(adEvent.ad)
                }
                is AdEvent.Shown -> listener.onAdShown(adEvent.ad)
                is AdEvent.PaidRevenue -> listener.onRevenuePaid(adEvent.ad, adEvent.adValue)
                is AdEvent.ShowFailed -> listener.onAdLoadFailed(adEvent.cause)
                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
    }
}

private const val Tag = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L