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
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
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
class BannerView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0,
    internal val auctionResult: AuctionResult = AuctionResult.UnknownAdapter("", AuctionResult.UnknownAdapter.Type.Network),
    private val demandAd: DemandAd = DemandAd(AdType.Banner),
) : FrameLayout(context, attrs, defStyleAtt),
    BannerAd,
    Extras by demandAd {

    override var format: BannerFormat = BannerFormat.Banner
        private set
    private var userListener: BannerListener? = null
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private val adLifecycleFlow = MutableStateFlow(AdLifecycle.Loaded)
    private val visibilityTracker: VisibilityTracker by lazy { get() }
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
        subscribeToWinner(auctionResult.adSource)
    }

    private var internalAdSize: AdSize? = null

    override val adSize: AdSize?
        get() = internalAdSize ?: (auctionResult.adSource as AdSource.Banner).getAdView()?.let { holder ->
            AdSize(widthDp = holder.widthDp, heightDp = holder.heightDp).also {
                internalAdSize = it
            }
        }

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        this.format = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
    }

    override fun isReady(): Boolean {
        return auctionResult.adSource.isAdReadyToShow
    }

    override fun showAd() {
        logInfo(TAG, "ShowAd invoked")
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
                    userListener?.onAdShowFailed(BidonError.BannerAdNotReady)
                    return
                }
                val bannerSource = (auctionResult.adSource as AdSource.Banner)
                // Success
                addViewOnScreen(bannerSource)
            }

            AdLifecycle.Displayed -> {
                // do nothing
            }

            AdLifecycle.LoadingFailed,
            AdLifecycle.DisplayingFailed,
            AdLifecycle.Destroyed -> {
                userListener?.onAdShowFailed(BidonError.BannerAdNotReady)
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
                    auctionResult.adSource.sendLoss(
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
            auctionResult.adSource.sendWin()
        }
    }

    override fun destroyAd() {
        adLifecycleFlow.value = AdLifecycle.Destroyed
        visibilityTracker.stop()
        auctionResult.adSource.destroy()
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

private const val TAG = "BannerView2"