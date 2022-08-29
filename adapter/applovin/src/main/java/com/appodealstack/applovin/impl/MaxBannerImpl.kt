package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.View
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
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.data.models.minByPricefloorOrNull
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

internal class MaxBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Banner<MaxBannerAuctionParams> {

    private var maxAdView: MaxAdView? = null
    private var maxAd: MaxAd? = null

    private val maxAdListener by lazy {
        object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            priceFloor = ad.revenue,
                            adSource = this@MaxBannerImpl
                        )
                    )
                )
            }

            override fun onAdExpanded(ad: MaxAd?) {}
            override fun onAdCollapsed(ad: MaxAd?) {}

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
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
        logInternal(Tag, "destroy")
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

    override fun getAdView(): View {
        return requireNotNull(maxAdView)
    }

    override suspend fun bid(
        adParams: MaxBannerAuctionParams
    ): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams")

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
            is AdState.LoadFailed -> state.cause.asFailure()
            is AdState.Bid -> state.result.asSuccess()
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
            currencyCode = USD
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
            currencyCode = USD
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
