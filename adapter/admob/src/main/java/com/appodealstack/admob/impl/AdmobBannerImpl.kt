package com.appodealstack.admob.impl

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.appodealstack.admob.AdmobBannerAuctionParams
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.data.models.minByPricefloorOrNull
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class AdmobBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Banner<AdmobBannerAuctionParams> {

    override val ad: Ad?
        get() = adView?.asAd()

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main

    private var lineItem: LineItem? = null
    private var adView: AdView? = null
    private val requiredAdView: AdView get() = requireNotNull(adView)

    private val requestListener by lazy {
        object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError. $this", loadAdError.asBidonError())
                adState.tryEmit(AdState.LoadFailed(loadAdError.asBidonError()))
            }

            override fun onAdLoaded() {
                logInfo(Tag, "onAdLoaded: $this")
                adView?.run {
                    adState.tryEmit(
                        AdState.Bid(
                            AuctionResult(
                                priceFloor = requireNotNull(lineItem?.priceFloor),
                                adSource = this@AdmobBannerImpl
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInternal(Tag, "onAdClicked: $this")
                adState.tryEmit(AdState.Clicked(requiredAdView.asAd()))
            }

            override fun onAdClosed() {
                logInternal(Tag, "onAdClosed: $this")
                adState.tryEmit(AdState.Closed(requiredAdView.asAd()))
            }

            override fun onAdImpression() {
                logInternal(Tag, "onAdImpression: $this")
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
            val valueMicros = adValue.valueMicros
            val ecpm = adValue.valueMicros / 1_000_000L
            logInternal(
                Tag,
                "OnPaidEventListener( ValueMicros=$valueMicros, $ecpm ${adValue.currencyCode}, $type )"
            )
        }
    }

    override fun destroy() {
        logInternal(Tag, "destroy $this")
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
        )
    }

    override suspend fun bid(adParams: AdmobBannerAuctionParams): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams")
        return withContext(dispatcher) {
            lineItem = adParams.lineItem
            val adUnitId = lineItem?.adUnitId
            if (!adUnitId.isNullOrBlank()) {
                val adView = AdView(adParams.adContainer.context)
                    .apply {
                        val admobBannerSize = adParams.bannerSize.asAdmobAdSize(adParams.adContainer)
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
                is AdState.LoadFailed -> state.cause.asFailure()
                is AdState.Bid -> state.result.asSuccess()
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInternal(Tag, "Starting fill: $this")
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(adView?.asAd())
        ).also { adState.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {}

    override fun getAdView(): View = requiredAdView

    private fun AdView.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD"
        )
    }

    private fun BannerSize.asAdmobAdSize(adContainer: ViewGroup) = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerSize.Large -> AdSize.LARGE_BANNER
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
