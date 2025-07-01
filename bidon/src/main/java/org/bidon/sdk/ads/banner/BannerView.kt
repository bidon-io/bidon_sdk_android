package org.bidon.sdk.ads.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.R
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.InitAwaiter
import org.bidon.sdk.ads.InitAwaiterImpl
import org.bidon.sdk.ads.banner.ext.height
import org.bidon.sdk.ads.banner.ext.width
import org.bidon.sdk.ads.banner.helper.AdLifecycle
import org.bidon.sdk.ads.banner.helper.LogLifecycleAdStateUseCase
import org.bidon.sdk.ads.banner.helper.wrapUserBannerListener
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.impl.asBidonErrorOrUnspecified
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import org.bidon.sdk.utils.ext.dpToPx
import org.bidon.sdk.utils.visibilitytracker.VisibilityTracker
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0,
    val auctionKey: String? = null,
    private val demandAd: DemandAd = DemandAd(AdType.Banner),
) : FrameLayout(context, attrs, defStyleAtt),
    InitAwaiter by InitAwaiterImpl(),
    BannerAd,
    Extras by demandAd {

    var format: BannerFormat = BannerFormat.Banner
        private set

    private var pricefloor: Double = BidonSdk.DefaultPricefloor
    private var userListener: BannerListener? = null
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private var loadingError: BidonError? = null
    private val adLifecycleFlow = MutableStateFlow(AdLifecycle.Created)
    private val auction: Auction by lazy { get() }
    private val visibilityTracker: VisibilityTracker by lazy { get() }
    private var auctionInfo: AuctionInfo? = null
    private var winner: AuctionResult? = null
        set(value) {
            wasNotified.set(false)
            field = value
        }
    private val wasNotified = AtomicBoolean(false)
    private var winnerSubscriberJob: Job? = null

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
    }

    private var internalAdSize: AdSize? = null

    override val adSize: AdSize
        get() = internalAdSize ?: AdSize(widthDp = format.width, heightDp = format.height)
            .also { internalAdSize = it }

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        this.format = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        logInfo(TAG, "LoadAd. $this. ${Thread.currentThread()}")
        scope.launch(Dispatchers.Default) {
            initWaitAndContinueIfRequired(
                onSuccess = {
                    if (adLifecycleFlow.compareAndSet(
                            expect = AdLifecycle.Created,
                            update = AdLifecycle.Loading
                        )
                    ) {
                        conductAuction(activity, pricefloor)
                    } else {
                        when (adLifecycleFlow.value) {
                            AdLifecycle.Loading -> {
                                logInfo(TAG, "Auction already in progress")
                                withContext(Dispatchers.Main) {
                                    userListener?.onAdLoadFailed(null, BidonError.AuctionInProgress)
                                }
                            }

                            AdLifecycle.Loaded -> {
                                winner?.adSource?.ad?.let {
                                    logInfo(TAG, "Banner loaded")
                                    withContext(Dispatchers.Main) {
                                        userListener?.onAdLoaded(
                                            ad = it,
                                            auctionInfo = requireNotNull(auctionInfo) {
                                                "[AuctionInfo] should exist when action succeeds"
                                            }
                                        )
                                    }
                                }
                            }

                            else -> {
                                logInfo(TAG, "Ad State=${adLifecycleFlow.value}")
                            }
                        }
                    }
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

    override fun isReady(): Boolean {
        return winner?.adSource?.isAdReadyToShow == true
    }

    override fun showAd() {
        logInfo(TAG, "ShowAd invoked. ${Thread.currentThread()}")
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            listener.onAdShowFailed(BidonError.SdkNotInitialized)
            return
        }
        when (adLifecycleFlow.value) {
            AdLifecycle.Displaying,
            AdLifecycle.Created,
            AdLifecycle.Loading -> {
                // do nothing
            }

            AdLifecycle.Loaded -> {
                val isLoaded =
                    isReady() && adLifecycleFlow.compareAndSet(
                        expect = AdLifecycle.Loaded,
                        update = AdLifecycle.Displaying
                    )
                if (!isLoaded) {
                    logInfo(TAG, "Not loaded. Current state: ${adLifecycleFlow.value}")
                    LogLifecycleAdStateUseCase.invoke(adLifecycle = adLifecycleFlow.value)
                    userListener?.onAdShowFailed(loadingError ?: BidonError.AdNotReady)
                    return
                }
                val bannerSource = (winner?.adSource as? AdSource.Banner) ?: run {
                    logInfo(TAG, "AdSource(${winner?.adSource}: no ad view.")
                    LogLifecycleAdStateUseCase.invoke(adLifecycle = adLifecycleFlow.value)
                    userListener?.onAdShowFailed(loadingError ?: BidonError.AdNotReady)
                    return
                }
                // Success
                addViewOnScreen(bannerSource)
            }

            AdLifecycle.Displayed -> {
                // do nothing
            }

            AdLifecycle.LoadingFailed,
            AdLifecycle.DisplayingFailed,
            AdLifecycle.Destroyed -> {
                userListener?.onAdShowFailed(loadingError ?: BidonError.AdNotReady)
            }
        }
    }

    override fun setBannerListener(listener: BannerListener?) {
        userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {
        logInfo(TAG, "Notify Loss invoked with Winner($winnerDemandId, $winnerPrice)")
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        when (adLifecycleFlow.value) {
            AdLifecycle.Loading -> {
                destroyAd()
                userListener?.onAdLoadFailed(null, BidonError.AuctionCancelled)
            }

            AdLifecycle.Loaded -> {
                if (!wasNotified.getAndSet(true)) {
                    winner?.adSource?.sendLoss(
                        winnerDemandId = winnerDemandId,
                        winnerPrice = winnerPrice,
                    )
                    destroyAd()
                }
            }

            else -> {
                // do nothing
            }
        }
    }

    override fun notifyWin() {
        logInfo(TAG, "Notify Win was invoked")
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        if (adLifecycleFlow.value == AdLifecycle.Loaded && !wasNotified.getAndSet(true)) {
            winner?.adSource?.sendWin()
        }
    }

    override fun destroyAd() {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            adLifecycleFlow.value = AdLifecycle.Destroyed
            visibilityTracker.stop()
            auction.cancel()
            winner?.adSource?.destroy()
            winner = null
            winnerSubscriberJob?.cancel()
            winnerSubscriberJob = null
            removeAllViews()
        }
    }

    private fun FrameLayout.addViewOnScreen(adSource: AdSource.Banner<*>) {
        // add AdView to Screen
        removeAllViews()
        val adViewHolder: AdViewHolder = adSource.getAdView() ?: run {
            logError(TAG, "No AdView found.", NullPointerException())
            adLifecycleFlow.value = AdLifecycle.DisplayingFailed
            listener.onAdShowFailed(BidonError.AdNotReady)
            return
        }
        val layoutParams = LayoutParams(adSize.widthDp.dpToPx, adSize.heightDp.dpToPx, Gravity.CENTER)
        addView(adViewHolder.networkAdview, layoutParams)
        this.visibility = VISIBLE
        adViewHolder.networkAdview.visibility = VISIBLE
        logInfo(TAG, "View added(${adSource.demandId.demandId}): ${adViewHolder.networkAdview}. Size(${adSize.widthDp}, ${adSize.heightDp})")
        val onBannerShown = {
            adLifecycleFlow.value = AdLifecycle.Displayed
            adSource.ad?.let { listener.onAdShown(ad = it) }
            adSource.sendShowImpression()
        }
        if (isVisibilityTrackingEnabled()) {
            checkBannerShown(
                networkAdview = adViewHolder.networkAdview,
                onBannerShown = onBannerShown
            )
        } else {
            onBannerShown.invoke()
        }
    }

    private fun conductAuction(activity: Activity, pricefloor: Double) {
        this.pricefloor = pricefloor
        logInfo(TAG, "Load (pricefloor=$pricefloor)")
        auction.start(
            demandAd = demandAd,
            adTypeParam = AdTypeParam.Banner(
                activity = activity,
                pricefloor = pricefloor,
                auctionKey = auctionKey,
                bannerFormat = format,
                containerWidth = width.toFloat()
            ),
            onSuccess = { winners, auctionInfo ->
                /**
                 * Winner found
                 */
                val winner = winners.first().also {
                    winner = it
                }
                this.auctionInfo = auctionInfo
                subscribeToWinner(winner.adSource)
                adLifecycleFlow.value = AdLifecycle.Loaded
                listener.onAdLoaded(
                    ad = requireNotNull(winner.adSource.ad) {
                        "[Ad] should exist when action succeeds"
                    },
                    auctionInfo = auctionInfo
                )
            },
            onFailure = { auctionInfo, cause ->
                /**
                 * Auction failed
                 */
                adLifecycleFlow.value = AdLifecycle.LoadingFailed
                loadingError = cause.asBidonErrorOrUnspecified()
                listener.onAdLoadFailed(
                    auctionInfo = auctionInfo,
                    cause = cause.asBidonErrorOrUnspecified()
                )
            }
        )
    }

    private fun subscribeToWinner(adSource: AdSource<*>) {
        winnerSubscriberJob = adSource.adEvent.onEach { adEvent ->
            logInfo(TAG, "$adEvent")
            when (adEvent) {
                is AdEvent.OnReward,
                is AdEvent.Closed,
                is AdEvent.LoadFailed,
                is AdEvent.Fill -> {
                    // do nothing
                }

                is AdEvent.Clicked -> {
                    listener.onAdClicked(adEvent.ad)
                    adSource.sendClickImpression()
                }

                is AdEvent.Shown -> {
                    // banners do not invoke onShown callback
                }

                is AdEvent.PaidRevenue -> listener.onRevenuePaid(adEvent.ad, adEvent.adValue)
                is AdEvent.ShowFailed -> {
                    adLifecycleFlow.value = AdLifecycle.DisplayingFailed
                    listener.onAdLoadFailed(null, adEvent.cause)
                }

                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
    }

    private fun isVisibilityTrackingEnabled(): Boolean {
        return when (val extra = getExtras()["ext"]) {
            is JSONObject -> extra.optBoolean("use_visibility_tracker", true)
            else -> true
        }
    }

    private fun checkBannerShown(networkAdview: View, onBannerShown: () -> Unit) {
        visibilityTracker.start(view = networkAdview) {
            onBannerShown.invoke()
        }
    }
}

private const val TAG = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L