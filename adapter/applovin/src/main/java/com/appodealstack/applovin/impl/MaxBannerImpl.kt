package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.MaxBannerAuctionParams
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.auction.minByPricefloorOrNull
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.data.models.stats.asRoundStatus
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.AdState
import com.appodealstack.bidon.domain.adapter.AdViewHolder
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.*
import com.appodealstack.bidon.domain.logging.impl.logError
import com.appodealstack.bidon.domain.logging.impl.logInfo
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.view.helper.impl.dpToPx
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class MaxBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<MaxBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var maxAdView: MaxAdView? = null
    private var maxAd: MaxAd? = null
    private var bannerSize: BannerSize? = null

    private val maxAdListener by lazy {
        object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                markBidFinished(
                    ecpm = requireNotNull(ad.revenue),
                    roundStatus = RoundStatus.Successful,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = ad.revenue,
                            adSource = this@MaxBannerImpl,
                        )
                    )
                )
            }

            override fun onAdExpanded(ad: MaxAd?) {}
            override fun onAdCollapsed(ad: MaxAd?) {}

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                markBidFinished(
                    ecpm = null,
                    roundStatus = error.asBidonError().asRoundStatus(),
                )
                adState.tryEmit(AdState.LoadFailed(error.asBidonError()))
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Impression(ad.asAd()))
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                adState.tryEmit(AdState.ShowFailed(error.asBidonError()))
            }
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = maxAd?.asAd() ?: maxAdView?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        maxAdView?.setListener(null)
        maxAdView?.destroy()
        maxAdView = null
        maxAd = null
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
        MaxBannerAuctionParams(
            context = adContainer.context,
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            bannerSize = bannerSize,
            adaptiveBannerHeight = null
        )
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(maxAdView)
        return AdViewHolder(
            networkAdview = adView,
            widthPx = FrameLayout.LayoutParams.MATCH_PARENT,
            heightPx = when (bannerSize) {
                BannerSize.Banner -> 50.dpToPx
                BannerSize.LeaderBoard -> 90.dpToPx
                BannerSize.MRec -> 250.dpToPx
                BannerSize.Adaptive,
                null -> FrameLayout.LayoutParams.WRAP_CONTENT
            }
        )
    }

    override suspend fun bid(adParams: MaxBannerAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams")
        markBidStarted(adParams.lineItem.adUnitId)
        bannerSize = adParams.bannerSize
        val maxAdView = if (adParams.bannerSize == BannerSize.Adaptive) {
            MaxAdView(
                adParams.lineItem.adUnitId,
                adParams.context,
            ).apply {
                val activity = adParams.context as Activity
                val heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).height
                val heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
                setExtraParameter("adaptive_banner", "true")
            }
        } else {
            MaxAdView(
                adParams.lineItem.adUnitId,
                adParams.bannerSize.asMaxAdFormat(),
                adParams.context,
            )
        }
        maxAdView.apply {
            setListener(maxAdListener)
            placement = demandAd.placement
            /**
             * AutoRefresher.kt provides auto-refresh
             */
            setExtraParameter("allow_pause_auto_refresh_immediately", "true")
            stopAutoRefresh()
        }.also {
            this.maxAdView = it
        }
        maxAdView.loadAd()
        val state = adState.first {
            it is AdState.Bid || it is AdState.LoadFailed
        }
        return when (state) {
            is AdState.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this
                )
            }
            is AdState.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Applovin fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(maxAdView?.asAd())
        ).also { adState.tryEmit(it) }.ad
    }

    override fun show(activity: Activity) {}

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = maxAd?.revenue ?: 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxAdView?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = 0.0,
            sourceAd = maxAd ?: demandAd,
            monetizationNetwork = null,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    private fun BannerSize.asMaxAdFormat() = when (this) {
        BannerSize.Banner -> MaxAdFormat.BANNER
        BannerSize.LeaderBoard -> MaxAdFormat.LEADER
        BannerSize.MRec -> MaxAdFormat.MREC
        else -> error(BidonError.AdFormatIsNotSupported(demandId.demandId, bannerSize = this))
    }
}

private const val Tag = "Max Banner"
private const val USD = "USD"
