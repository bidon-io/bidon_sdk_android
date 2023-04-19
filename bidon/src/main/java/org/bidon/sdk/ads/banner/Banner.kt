package org.bidon.sdk.ads.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.R
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.asUnspecified
import org.bidon.sdk.ads.banner.helper.AdLifecycle
import org.bidon.sdk.ads.banner.helper.LogLifecycleAdStateUseCase
import org.bidon.sdk.ads.banner.helper.wrapUserBannerListener
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.BannerRequestBody.Companion.asStatBannerFormat
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
class Banner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAtt: Int = 0,
    private val demandAd: DemandAd = DemandAd(AdType.Banner),
) : FrameLayout(context, attrs, defStyleAtt),
    BannerAd,
    Extras by demandAd {

    private var bannerFormat: BannerFormat = BannerFormat.Banner
    private var pricefloor: Double = BidonSdk.DefaultPricefloor

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

    private var userListener: BannerListener? = null
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private val adLifecycleFlow = MutableStateFlow(AdLifecycle.Created)
    private val auction: Auction get() = get()
    private var winner: AuctionResult? = null
    private var winnerSubscriberJob: Job? = null

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        this.bannerFormat = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        if (adLifecycleFlow.compareAndSet(expect = AdLifecycle.Created, update = AdLifecycle.Loading)) {
            this.pricefloor = pricefloor
            logInfo(Tag, "Load (pricefloor=$pricefloor)")
            scope.launch {
                auction.start(
                    demandAd = demandAd,
                    resolver = MaxEcpmAuctionResolver,
                    adTypeParamData = AdTypeParam.Banner(
                        activity = activity,
                        pricefloor = pricefloor,
                        bannerFormat = bannerFormat,
                        containerWidth = width.toFloat()
                    ),
                ).onSuccess { auctionResults ->
                    /**
                     * Winner found
                     */
                    val winner = auctionResults.first().also {
                        winner = it
                    }
                    subscribeToWinner(winner.adSource)
                    listener.onAdLoaded(
                        requireNotNull(winner.adSource.ad) {
                            "[Ad] should exist when action succeeds"
                        }
                    )
                    adLifecycleFlow.value = AdLifecycle.Loaded
                }.onFailure {
                    /**
                     * Auction failed
                     */
                    listener.onAdLoadFailed(cause = it.asUnspecified())
                    adLifecycleFlow.value = AdLifecycle.LoadingFailed
                }
            }
        } else {
            logInfo(Tag, "Auction already in progress")
            userListener?.onAdLoadFailed(BidonError.AuctionInProgress)
        }
    }

    override fun isReady(): Boolean {
        return winner?.adSource?.isAdReadyToShow == true
    }

    override fun showAd() {
        logInfo(Tag, "ShowAd invoked")
        val adViewHolder = (winner?.adSource as? AdSource.Banner)?.getAdView() ?: run {
            LogLifecycleAdStateUseCase.invoke(adLifecycle = adLifecycleFlow.value)
            userListener?.onAdLoadFailed(BidonError.BannerAdNotReady)
            return
        }
        val isLoaded = adLifecycleFlow.compareAndSet(expect = AdLifecycle.Loaded, update = AdLifecycle.Displaying)
        if (!isLoaded) {
            LogLifecycleAdStateUseCase.invoke(adLifecycle = adLifecycleFlow.value)
            userListener?.onAdLoadFailed(BidonError.BannerAdNotReady)
            return
        }
        // add AdView to Screen
        removeAllViews()
        val layoutParams = LayoutParams(adViewHolder.widthPx, adViewHolder.heightPx).apply {
            gravity = Gravity.CENTER
        }
        addView(adViewHolder.networkAdview, layoutParams)
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {
        logInfo(Tag, "Notify Loss invoked with Winner($winnerDemandId, $winnerEcpm)")
        winner?.adSource?.sendLoss(
            winnerDemandId = winnerDemandId,
            winnerEcpm = winnerEcpm,
            adType = StatisticsCollector.AdType.Banner(
                format = bannerFormat.asStatBannerFormat()
            )
        )
        destroyAd()
    }

    override fun destroyAd() {
        adLifecycleFlow.value = AdLifecycle.Destroyed
        winner?.adSource?.destroy()
        winner = null
        winnerSubscriberJob?.cancel()
        winnerSubscriberJob = null
        removeAllViews()
    }

    override fun setBannerListener(listener: BannerListener) {
        userListener = listener
    }

    private fun subscribeToWinner(adSource: AdSource<*>) {
        winnerSubscriberJob = adSource.adEvent.onEach { adEvent ->
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
                    adSource.sendClickImpression(
                        StatisticsCollector.AdType.Banner(
                            format = bannerFormat.asStatBannerFormat()
                        )
                    )
                }
                is AdEvent.Shown -> {
                    adLifecycleFlow.value = AdLifecycle.Displayed
                    listener.onAdShown(adEvent.ad)
                    adSource.sendShowImpression(
                        StatisticsCollector.AdType.Banner(
                            format = bannerFormat.asStatBannerFormat()
                        )
                    )
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
}

private const val Tag = "Banner"
const val DefaultAutoRefreshTimeoutMs = 10_000L