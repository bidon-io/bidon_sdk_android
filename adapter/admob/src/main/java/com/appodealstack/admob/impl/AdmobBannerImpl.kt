package com.appodealstack.admob.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.appodealstack.admob.AdmobBannerAuctionParams
import com.appodealstack.admob.asBidonError
import com.appodealstack.admob.ext.asBidonAdValue
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.config.BidonError
import com.appodealstack.bidon.logs.analytic.AdValue
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.stats.models.RoundStatus
import com.appodealstack.bidon.stats.models.asRoundStatus
import com.appodealstack.bidon.utils.SdkDispatchers
import com.google.android.gms.ads.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class AdmobBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<AdmobBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    override val ad: Ad?
        get() = adView?.asAd()

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override var isAdReadyToShow: Boolean = false

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var adSize: AdSize? = null
    private var lineItem: LineItem? = null
    private var adView: AdView? = null
    private val requiredAdView: AdView get() = requireNotNull(adView)

    private val requestListener by lazy {
        object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError. $this", loadAdError.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = loadAdError.asBidonError().asRoundStatus(),
                )
                adEvent.tryEmit(
                    AdEvent.LoadFailed(loadAdError.asBidonError())
                )
            }

            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded: $this")
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.pricefloor),
                    roundStatus = RoundStatus.Successful,
                )
                adView?.run {
                    isAdReadyToShow = true
                    adEvent.tryEmit(
                        AdEvent.Bid(
                            AuctionResult(
                                ecpm = requireNotNull(lineItem?.pricefloor),
                                adSource = this@AdmobBannerImpl,
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(requiredAdView.asAd()))
            }

            override fun onAdClosed() {
                logInfo(Tag, "onAdClosed: $this")
                adEvent.tryEmit(AdEvent.Closed(requiredAdView.asAd()))
            }

            override fun onAdImpression() {
                logInfo(Tag, "onAdShown: $this")
                adEvent.tryEmit(AdEvent.Shown(requiredAdView.asAd()))
            }

            override fun onAdOpened() {}
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            adEvent.tryEmit(
                AdEvent.PaidRevenue(
                    ad = Ad(
                        demandAd = demandAd,
                        eCPM = lineItem?.pricefloor ?: 0.0,
                        sourceAd = requiredAdView,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = AdValue.DefaultCurrency,
                        auctionId = auctionId,
                        adUnitId = lineItem?.adUnitId
                    ),
                    adValue = adValue.asBidonAdValue()
                )
            )
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adView?.onPaidEventListener = null
        adView = null
        lineItem = null
    }

    override fun getAuctionParams(
        adContainer: ViewGroup,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed)
        AdmobBannerAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            bannerFormat = bannerFormat,
            adContainer = adContainer,
            pricefloor = pricefloor
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun bid(adParams: AdmobBannerAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        return withContext(dispatcher) {
            lineItem = adParams.lineItem
            val adUnitId = lineItem?.adUnitId
            if (!adUnitId.isNullOrBlank()) {
                val adView = AdView(adParams.adContainer.context)
                    .apply {
                        val admobBannerSize = adParams.bannerFormat.asAdmobAdSize(adParams.adContainer)
                        this@AdmobBannerImpl.adSize = admobBannerSize
                        this.setAdSize(admobBannerSize)
                        this.adUnitId = adUnitId
                        this.adListener = requestListener
                        this.onPaidEventListener = paidListener
                    }
                    .also {
                        adView = it
                    }
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            } else {
                val error = BidonError.NoAppropriateAdUnitId
                logError(
                    tag = Tag,
                    message = "No appropriate AdUnitId found for price_floor=${adParams.lineItem.pricefloor}",
                    error = error
                )
                adEvent.tryEmit(AdEvent.LoadFailed(error))
            }
            val state = adEvent.first {
                it is AdEvent.Bid || it is AdEvent.LoadFailed
            }
            when (state) {
                is AdEvent.LoadFailed -> {
                    AuctionResult(
                        ecpm = 0.0,
                        adSource = this@AdmobBannerImpl
                    )
                }
                is AdEvent.Bid -> state.result
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdEvent.Fill(
            requireNotNull(adView?.asAd())
        ).also {
            markFillFinished(RoundStatus.Successful)
            adEvent.tryEmit(it)
        }.ad
    }

    override fun show(activity: Activity) {}

    override fun getAdView(): AdViewHolder = AdViewHolder(
        networkAdview = requiredAdView,
        widthPx = FrameLayout.LayoutParams.MATCH_PARENT,
        heightPx = adSize?.getHeightInPixels(requiredAdView.context) ?: FrameLayout.LayoutParams.WRAP_CONTENT
    )

    private fun AdView.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            eCPM = lineItem?.pricefloor ?: 0.0,
            sourceAd = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = adUnitId
        )
    }

    private fun BannerFormat.asAdmobAdSize(adContainer: ViewGroup) = when (this) {
        BannerFormat.Banner -> AdSize.BANNER
        BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
        BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerFormat.Adaptive -> adContainer.adaptiveAdSize()
    }

    @Suppress("DEPRECATION")
    private fun ViewGroup.adaptiveAdSize(): AdSize {
        val windowManager = this.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = this.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this.context, adWidth)
    }
}

private const val Tag = "Admob Banner"
