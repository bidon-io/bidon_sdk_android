package com.appodealstack.admob.impl

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.appodealstack.admob.AdmobBannerAuctionParams
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.AdState
import com.appodealstack.bidon.adapter.AdViewHolder
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.adapter.DemandId
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.banner.BannerSize
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.config.BidonError
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

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

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
                adState.tryEmit(
                    AdState.LoadFailed(loadAdError.asBidonError())
                )
            }

            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded: $this")
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.priceFloor),
                    roundStatus = RoundStatus.Successful,
                )
                adView?.run {
                    adState.tryEmit(
                        AdState.Bid(
                            AuctionResult(
                                ecpm = requireNotNull(lineItem?.priceFloor),
                                adSource = this@AdmobBannerImpl,
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked: $this")
                adState.tryEmit(AdState.Clicked(requiredAdView.asAd()))
            }

            override fun onAdClosed() {
                logInfo(Tag, "onAdClosed: $this")
                adState.tryEmit(AdState.Closed(requiredAdView.asAd()))
            }

            override fun onAdImpression() {
                logInfo(Tag, "onAdShown: $this")
                adState.tryEmit(AdState.Impression(requiredAdView.asAd()))
            }

            override fun onAdOpened() {}
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            val type = when (adValue.precisionType) {
                0 -> "UNKNOWN"
                1 -> "PRECISE"
                2 -> "ESTIMATED"
                3 -> "PUBLISHER_PROVIDED"
                else -> "unknown type ${adValue.precisionType}"
            }
            val ecpm = adValue.valueMicros / 1_000_000.0
            adState.tryEmit(
                AdState.PaidRevenue(
                    ad = Ad(
                        demandId = demandId,
                        demandAd = demandAd,
                        price = ecpm,
                        sourceAd = requiredAdView,
                        monetizationNetwork = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = "USD",
                        auctionId = auctionId,
                    )
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
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerSize: BannerSize,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
            ?.also(onLineItemConsumed)
        AdmobBannerAuctionParams(
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            bannerSize = bannerSize,
            adContainer = adContainer,
            priceFloor = priceFloor
        )
    }

    override suspend fun bid(adParams: AdmobBannerAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        return withContext(dispatcher) {
            lineItem = adParams.lineItem
            val adUnitId = lineItem?.adUnitId
            if (!adUnitId.isNullOrBlank()) {
                val adView = AdView(adParams.adContainer.context)
                    .apply {
                        val admobBannerSize = adParams.bannerSize.asAdmobAdSize(adParams.adContainer)
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
                    message = "No appropriate AdUnitId found for price_floor=${adParams.lineItem.priceFloor}",
                    error = error
                )
                adState.tryEmit(AdState.LoadFailed(error))
            }
            val state = adState.first {
                it is AdState.Bid || it is AdState.LoadFailed
            }
            when (state) {
                is AdState.LoadFailed -> {
                    AuctionResult(
                        ecpm = 0.0,
                        adSource = this@AdmobBannerImpl
                    )
                }
                is AdState.Bid -> state.result
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
        AdState.Fill(
            requireNotNull(adView?.asAd())
        ).also {
            markFillFinished(RoundStatus.Successful)
            adState.tryEmit(it)
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
            demandId = demandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD",
            auctionId = auctionId,
        )
    }

    private fun BannerSize.asAdmobAdSize(adContainer: ViewGroup) = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerSize.Adaptive -> adContainer.adaptiveAdSize()
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
