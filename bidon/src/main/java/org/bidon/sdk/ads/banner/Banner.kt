package org.bidon.sdk.ads.banner

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
import org.bidon.sdk.ads.banner.helper.wrapUserBannerListener
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Bidon Team on 02/03/2023.
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

    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private val isAuctionStarted = AtomicBoolean(false)
    private var userListener: BannerListener? = null
    private val listener by lazy { wrapUserBannerListener(userListener = { userListener }) }
    private var subscriberJob: Job? = null
    private val auction: Auction by lazy {
        get()
    }
    private var winner: AuctionResult? = null
    private var shown = AtomicBoolean(false)

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

    override fun setBannerFormat(bannerFormat: BannerFormat) {
        this.bannerFormat = bannerFormat
    }

    override fun loadAd(pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            logInfo(Tag, "Sdk is not initialized")
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        if (!isAuctionStarted.getAndSet(true)) {
            this.pricefloor = pricefloor
            logInfo(Tag, "Load (pricefloor=$pricefloor)")
            scope.launch {
                auction.start(
                    demandAd = demandAd,
                    resolver = MaxEcpmAuctionResolver,
                    adTypeParamData = AdTypeParam.Banner(
                        pricefloor = pricefloor,
                        bannerFormat = bannerFormat,
                        adContainer = this@Banner,
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
                }.onFailure {
                    /**
                     * Auction failed
                     */
                    listener.onAdLoadFailed(cause = it.asUnspecified())
                }
            }
        } else {
            logInfo(Tag, "Auction already in progress")
        }
    }

    override fun isReady(): Boolean {
        return winner?.adSource?.isAdReadyToShow == true
    }

    override fun showAd() {
        val adViewHolder = (winner?.adSource as? AdSource.Banner)?.getAdView()
        when {
            adViewHolder != null && !shown.getAndSet(true) -> {
                logInfo(Tag, "Show")
                // add AdView to Screen
                removeAllViews()
                val layoutParams = LayoutParams(adViewHolder.widthPx, adViewHolder.heightPx).apply {
                    gravity = Gravity.CENTER
                }
                addView(adViewHolder.networkAdview, layoutParams)
            }
            shown.get() -> {
                logInfo(Tag, "Banner already shown")
            }
            else -> {
                logInfo(Tag, "Banner not loaded")
            }
        }
    }

    override fun destroyAd() {
        winner?.adSource?.destroy()
        winner = null
        subscriberJob?.cancel()
        subscriberJob = null
        removeAllViews()
    }

    override fun setBannerListener(listener: BannerListener) {
        userListener = listener
    }

    private fun subscribeToWinner(adSource: AdSource<*>) {
        subscriberJob = adSource.adEvent.onEach { adEvent ->
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