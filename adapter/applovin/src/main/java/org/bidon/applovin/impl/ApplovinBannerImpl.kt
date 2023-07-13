package org.bidon.applovin.impl

import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinSdk
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.applovin.ApplovinBannerAuctionParams
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
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * I have no idea how it works. There is no documentation.
 *
 * https://appodeal.slack.com/archives/C02PE4GAFU0/p1661421318406689
 */
internal class ApplovinBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val applovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Banner<ApplovinBannerAuctionParams>,
    AdLoadingType.Network<ApplovinBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var adView: AppLovinAdView? = null
    private var applovinAd: AppLovinAd? = null
    private var param: ApplovinBannerAuctionParams? = null

    private val listener by lazy {
        object : AppLovinAdDisplayListener, AppLovinAdClickListener {
            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $ad")
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = param?.lineItem?.pricefloor.asBidonAdValue()
                    )
                )
                // tracked impression/shown by [BannerView]
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $ad")
                adEvent.tryEmit(AdEvent.ShowFailed(BidonError.NoFill(demandId)))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $ad")
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val adEvent =
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: adView?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adView?.setAdLoadListener(null)
        adView = null
        applovinAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed)
            ApplovinBannerAuctionParams(
                context = activity.applicationContext,
                lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat
            )
        }
    }

    override fun fill(adParams: ApplovinBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        param = adParams
        val adSize = adParams.bannerFormat.asApplovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerFormat
            )
        )
        val bannerView =
            AppLovinAdView(applovinSdk, adSize, adParams.lineItem.adUnitId, adParams.context).also {
                it.setAdClickListener(listener)
                it.setAdDisplayListener(listener)
                adView = it
            }
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(Tag, "adReceived: $this")
                applovinAd = ad
                adEvent.tryEmit(AdEvent.Fill(requireNotNull(ad.asAd())))
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(Tag, "failedToReceiveAd: errorCode=$errorCode. $this")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        bannerView.setAdLoadListener(requestListener)
        bannerView.loadNextAd()
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(adView)
        return AdViewHolder(
            networkAdview = adView,
            widthDp = adView.size.width.takeIf { it > 0 } ?: when (param?.bannerFormat) {
                BannerFormat.Banner -> 320
                BannerFormat.LeaderBoard -> 728
                BannerFormat.MRec -> 300
                BannerFormat.Adaptive -> if (isTablet) 728 else 320
                null -> error("unexpected")
            },
            heightDp = adView.size.height
        )
    }

    private fun AppLovinAdView?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = param?.lineItem?.adUnitId
        )
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = param?.lineItem?.adUnitId
        )
    }

    private fun BannerFormat.asApplovinAdSize() = when (this) {
        BannerFormat.Banner -> AppLovinAdSize.BANNER
        BannerFormat.LeaderBoard -> AppLovinAdSize.LEADER
        BannerFormat.Adaptive -> if (isTablet) {
            AppLovinAdSize.LEADER
        } else {
            AppLovinAdSize.BANNER
        }

        BannerFormat.MRec -> AppLovinAdSize.MREC
    }
}

private const val Tag = "ApplovinBanner"
