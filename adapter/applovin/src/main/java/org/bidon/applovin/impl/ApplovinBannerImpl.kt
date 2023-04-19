package org.bidon.applovin.impl

import android.app.Activity
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.bidon.applovin.ApplovinBannerAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.asRoundStatus

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
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd,
    ) {

    private var adView: AppLovinAdView? = null
    private var applovinAd: AppLovinAd? = null
    private var param: ApplovinBannerAuctionParams? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(Tag, "adReceived: $this")
                applovinAd = ad
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = param?.lineItem?.pricefloor ?: 0.0,
                            adSource = this@ApplovinBannerImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(Tag, "failedToReceiveAd: errorCode=$errorCode. $this")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
    }

    private val listener by lazy {
        object : AppLovinAdDisplayListener, AppLovinAdClickListener {
            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $ad")
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = param?.lineItem?.pricefloor.asBidonAdValue()
                    )
                )
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $ad")
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $ad")
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: adView?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adView = null
        applovinAd = null
    }

    override fun getAuctionParams(
        adContainer: ViewGroup,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
            ?.also(onLineItemConsumed)
        ApplovinBannerAuctionParams(
            context = adContainer.context,
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            adaptiveBannerHeight = null,
            bannerFormat = bannerFormat
        )
    }

    override suspend fun bid(
        adParams: ApplovinBannerAuctionParams
    ): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        param = adParams
        val adSize = adParams.bannerFormat.asApplovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerFormat
            )
        )
        val bannerView = AppLovinAdView(applovinSdk, adSize, adParams.lineItem.adUnitId, adParams.context).also {
            it.setAdClickListener(listener)
            it.setAdDisplayListener(listener)
            adView = it
        }

        bannerView.setAdLoadListener(requestListener)
        bannerView.loadNextAd()

        val state = adEvent.first {
            it is AdEvent.Bid || it is AdEvent.LoadFailed
        }
        return when (state) {
            is AdEvent.LoadFailed -> {
                AuctionResult(
                    ecpm = 0.0,
                    adSource = this,
                    roundStatus = state.cause.asRoundStatus()
                )
            }
            is AdEvent.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        requireNotNull(applovinAd?.asAd()).also {
            adEvent.tryEmit(AdEvent.Fill(it))
        }
    }

    override fun show(activity: Activity) {
    }

    override fun getAdView(): AdViewHolder {
        val adView = requireNotNull(adView)
        return AdViewHolder(
            networkAdview = adView,
            widthPx = adView.size.width,
            heightPx = adView.size.height
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
        BannerFormat.Adaptive -> AppLovinAdSize.BANNER
        BannerFormat.LeaderBoard -> AppLovinAdSize.LEADER
        BannerFormat.MRec -> AppLovinAdSize.MREC
    }
}

private const val Tag = "ApplovinBanner"
