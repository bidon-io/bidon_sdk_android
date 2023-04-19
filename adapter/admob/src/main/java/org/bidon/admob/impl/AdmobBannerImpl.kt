package org.bidon.admob.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus
import org.bidon.sdk.utils.SdkDispatchers

internal class AdmobBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<AdmobBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    override val ad: Ad?
        get() = adView?.asAd()

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override var isAdReadyToShow: Boolean = false

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var adSize: AdSize? = null
    private var param: AdmobBannerAuctionParams? = null
    private var adView: AdView? = null
    private val requiredAdView: AdView get() = requireNotNull(adView)

    private val requestListener by lazy {
        object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError. $this", loadAdError.asBidonError())
                adEvent.tryEmit(
                    AdEvent.LoadFailed(loadAdError.asBidonError())
                )
            }

            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded: $this")
                adView?.run {
                    isAdReadyToShow = true
                    adEvent.tryEmit(
                        AdEvent.Bid(
                            AuctionResult(
                                ecpm = requireNotNull(param?.lineItem?.pricefloor),
                                adSource = this@AdmobBannerImpl,
                                roundStatus = RoundStatus.Successful
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
                        ecpm = param?.lineItem?.pricefloor ?: 0.0,
                        demandAdObject = requiredAdView,
                        networkName = demandId.demandId,
                        dsp = null,
                        roundId = roundId,
                        currencyCode = AdValue.USD,
                        auctionId = auctionId,
                        adUnitId = param?.lineItem?.adUnitId
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
        param = null
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
        return withContext(dispatcher) {
            param = adParams
            val adUnitId = param?.lineItem?.adUnitId
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
                        ecpm = adParams.lineItem.pricefloor,
                        adSource = this@AdmobBannerImpl,
                        roundStatus = state.cause.asRoundStatus()
                    )
                }
                is AdEvent.Bid -> state.result
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdEvent.Fill(
            requireNotNull(adView?.asAd())
        ).also {
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
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
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
