package org.bidon.sdk.ads.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.R
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.AdLifecycle
import org.bidon.sdk.ads.banner.helper.LogLifecycleAdStateUseCase
import org.bidon.sdk.ads.banner.helper.impl.dpToPx
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
import org.bidon.sdk.utils.visibilitytracker.VisibilityTracker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0,
    private val demandAd: DemandAd = DemandAd(AdType.Banner),
) : FrameLayout(context, attrs, defStyleAtt),
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

    override val adSize: AdSize?
        get() = internalAdSize ?: (winner?.adSource as? AdSource.Banner)?.getAdView()?.let { holder ->
            AdSize(widthDp = holder.widthDp, heightDp = holder.heightDp).also {
                internalAdSize = it
            }
        }

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        this.format = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        logInfo(TAG, "LoadAd. $this. ${Thread.currentThread()}")
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
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
                    userListener?.onAdLoadFailed(BidonError.AuctionInProgress)
                }

                AdLifecycle.Loaded -> {
                    winner?.adSource?.ad?.let {
                        logInfo(TAG, "Banner loaded")
                        userListener?.onAdLoaded(it)
                    }
                }

                else -> {
                    logInfo(TAG, "Ad State=${adLifecycleFlow.value}")
                }
            }
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
                    userListener?.onAdShowFailed(loadingError ?: BidonError.BannerAdNotReady)
                    return
                }
                val bannerSource = (winner?.adSource as? AdSource.Banner) ?: run {
                    logInfo(TAG, "AdSource(${winner?.adSource}: no ad view.")
                    LogLifecycleAdStateUseCase.invoke(adLifecycle = adLifecycleFlow.value)
                    userListener?.onAdShowFailed(loadingError ?: BidonError.BannerAdNotReady)
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
                userListener?.onAdShowFailed(loadingError ?: BidonError.BannerAdNotReady)
            }
        }
    }

    override fun setBannerListener(listener: BannerListener?) {
        userListener = listener
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {
        logInfo(TAG, "Notify Loss invoked with Winner($winnerDemandId, $winnerEcpm)")
        when (adLifecycleFlow.value) {
            AdLifecycle.Loading -> {
                destroyAd()
                userListener?.onAdLoadFailed(BidonError.AuctionCancelled)
            }

            AdLifecycle.Loaded -> {
                if (!wasNotified.getAndSet(true)) {
                    winner?.adSource?.sendLoss(
                        winnerDemandId = winnerDemandId,
                        winnerEcpm = winnerEcpm,
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
        if (adLifecycleFlow.value == AdLifecycle.Loaded && !wasNotified.getAndSet(true)) {
            winner?.adSource?.sendWin()
        }
    }

    override fun destroyAd() {
        adLifecycleFlow.value = AdLifecycle.Destroyed
        visibilityTracker.stop()
        auction.cancel()
        winner?.adSource?.destroy()
        winner = null
        winnerSubscriberJob?.cancel()
        winnerSubscriberJob = null
        removeAllViews()
    }

    private fun FrameLayout.addViewOnScreen(adSource: AdSource.Banner<*>) {
        // add AdView to Screen
        removeAllViews()
        val adViewHolder: AdViewHolder = adSource.getAdView() ?: run {
            logError(TAG, "No AdView found.", NullPointerException())
            return
        }
        val layoutParams = LayoutParams(adViewHolder.widthDp.dpToPx, adViewHolder.heightDp.dpToPx, Gravity.CENTER)
        addView(adViewHolder.networkAdview, layoutParams)
        this.visibility = VISIBLE
        adViewHolder.networkAdview.visibility = VISIBLE
        logInfo(
            TAG,
            "View added(${adSource.demandId.demandId}): ${adViewHolder.networkAdview}. Size(${adViewHolder.widthDp}, ${adViewHolder.heightDp})"
        )
        checkBannerShown(adViewHolder.networkAdview, onBannerShown = {
            adLifecycleFlow.value = AdLifecycle.Displayed
            adSource.ad?.let { listener.onAdShown(ad = it) }
            adSource.sendShowImpression()
        })
    }

    private fun conductAuction(activity: Activity, pricefloor: Double) {
        this.pricefloor = pricefloor
        logInfo(TAG, "Load (pricefloor=$pricefloor)")
        auction.start(
            demandAd = demandAd,
            adTypeParamData = AdTypeParam.Banner(
                activity = activity,
                pricefloor = pricefloor,
                bannerFormat = format,
                containerWidth = width.toFloat()
            ),
            onSuccess = { auctionResults ->
                /**
                 * Winner found
                 */
                val winner = auctionResults.first().also {
                    winner = it
                }
                subscribeToWinner(winner.adSource)
                adLifecycleFlow.value = AdLifecycle.Loaded
                listener.onAdLoaded(
                    requireNotNull(winner.adSource.ad) {
                        "[Ad] should exist when action succeeds"
                    }
                )
            },
            onFailure = {
                /**
                 * Auction failed
                 */
                adLifecycleFlow.value = AdLifecycle.LoadingFailed
                loadingError = it.asBidonErrorOrUnspecified()
                listener.onAdLoadFailed(cause = it.asBidonErrorOrUnspecified())
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
                    listener.onAdLoadFailed(adEvent.cause)
                }

                is AdEvent.Expired -> listener.onAdExpired(adEvent.ad)
            }
        }.launchIn(scope)
    }

    private fun checkBannerShown(networkAdview: View, onBannerShown: () -> Unit) {
        visibilityTracker.start(view = networkAdview) {
            onBannerShown.invoke()
        }
    }
}

private const val TAG = "BannerView"
const val DefaultAutoRefreshTimeoutMs = 10_000L