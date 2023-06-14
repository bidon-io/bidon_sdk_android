package org.bidon.applovin.impl

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.applovin.ApplovinDemandId
import org.bidon.applovin.MaxBannerAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class MaxBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String
) : AdSource.Banner<MaxBannerAuctionParams>,
    AdLoadingType.Network<MaxBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var maxAdView: MaxAdView? = null
    private var maxAd: MaxAd? = null
    private var bannerFormat: BannerFormat? = null

    private val maxAdListener by lazy {
        object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Fill(requireNotNull(maxAdView?.asAd())))
            }

            override fun onAdExpanded(ad: MaxAd?) {}
            override fun onAdCollapsed(ad: MaxAd?) {}

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                logError(Tag, "(code=${error.code}) ${error.message}", error.asBidonError())
                adEvent.tryEmit(AdEvent.LoadFailed(error.asBidonError()))
            }

            override fun onAdDisplayed(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = ad.asBidonAdValue()
                    )
                )
                // tracked impression/shown by [BannerView]
            }

            override fun onAdHidden(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun onAdClicked(ad: MaxAd) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                maxAd = ad
                adEvent.tryEmit(AdEvent.ShowFailed(error.asBidonError()))
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = maxAd != null

    override val ad: Ad?
        get() = maxAd?.asAd() ?: maxAdView?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        maxAdView?.setListener(null)
        maxAdView?.destroy()
        maxAdView = null
        maxAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed)
            MaxBannerAuctionParams(
                context = activity.applicationContext,
                lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat,
                adaptiveBannerHeight = null
            )
        }
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(maxAdView)
        return AdViewHolder(
            networkAdview = adView,
            widthDp = adView.width,
            heightDp = adView.height
        )
    }

    override fun fill(adParams: MaxBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams")
        bannerFormat = adParams.bannerFormat
        val maxAdView = if (adParams.bannerFormat == BannerFormat.Adaptive) {
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
                adParams.bannerFormat.asMaxAdFormat(),
                adParams.context,
            )
        }
        maxAdView.apply {
            setListener(maxAdListener)
            /**
             * AutoRefresher.kt provides auto-refresh
             */
            setExtraParameter("allow_pause_auto_refresh_immediately", "true")
            stopAutoRefresh()
        }.also {
            this.maxAdView = it
        }
        maxAdView.loadAd()
    }

    /**
     * Use it after loaded ECPM is known
     */
    private fun MaxAd?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            ecpm = maxAd?.revenue ?: 0.0,
            demandAdObject = maxAd ?: demandAd,
            networkName = maxAd?.networkName,
            dsp = maxAd?.dspId,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = maxAd?.adUnitId
        )
    }

    /**
     * Use it before loaded ECPM is unknown
     */
    private fun MaxAdView?.asAd(): Ad {
        val maxAd = this
        return Ad(
            demandAd = demandAd,
            ecpm = 0.0,
            demandAdObject = maxAd ?: demandAd,
            networkName = ApplovinDemandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = maxAd?.adUnitId
        )
    }

    private fun BannerFormat.asMaxAdFormat() = when (this) {
        BannerFormat.Banner -> MaxAdFormat.BANNER
        BannerFormat.LeaderBoard -> MaxAdFormat.LEADER
        BannerFormat.MRec -> MaxAdFormat.MREC
        else -> error(BidonError.AdFormatIsNotSupported(demandId.demandId, bannerFormat = this))
    }
}

private const val Tag = "Max Banner"
