package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinBannerAuctionParams
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.AdState
import com.appodealstack.bidon.adapter.AdViewHolder
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.BidonError
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.bidon.ads.DemandId
import com.appodealstack.bidon.ads.banner.BannerSize
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.StatisticsCollector
import com.appodealstack.bidon.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.stats.models.RoundStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

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
        demandId = demandId
    ) {

    private var adView: AppLovinAdView? = null
    private var applovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(Tag, "adReceived: $this")
                applovinAd = ad
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.priceFloor),
                    roundStatus = RoundStatus.Successful,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = lineItem?.priceFloor ?: 0.0,
                            adSource = this@ApplovinBannerImpl,
                        )
                    )
                )
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(Tag, "failedToReceiveAd: errorCode=$errorCode. $this")
                markBidFinished(
                    ecpm = null,
                    roundStatus = RoundStatus.NoBid,
                )
                adState.tryEmit(AdState.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
    }

    private val listener by lazy {
        object : AppLovinAdDisplayListener, AppLovinAdClickListener {
            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $ad")
                adState.tryEmit(AdState.Impression(ad.asAd()))
                adState.tryEmit(AdState.PaidRevenue(ad.asAd()))
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $ad")
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $ad")
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: adView?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        adView = null
        applovinAd = null
    }

    override fun getAuctionParams(
        adContainer: ViewGroup,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerSize: BannerSize,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
            ?.also(onLineItemConsumed)
        ApplovinBannerAuctionParams(
            context = adContainer.context,
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            adaptiveBannerHeight = null,
            bannerSize = bannerSize
        )
    }

    override suspend fun bid(
        adParams: ApplovinBannerAuctionParams
    ): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
        val adSize = adParams.bannerSize.asApplovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerSize
            )
        )
        val bannerView = AppLovinAdView(applovinSdk, adSize, adParams.lineItem.adUnitId, adParams.context).also {
            it.setAdClickListener(listener)
            it.setAdDisplayListener(listener)
            adView = it
        }

        bannerView.setAdLoadListener(requestListener)
        bannerView.loadNextAd()

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
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
        requireNotNull(applovinAd?.asAd()).also {
            markFillFinished(RoundStatus.Successful)
            adState.tryEmit(AdState.Fill(it))
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
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandId = ApplovinDemandId,
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            monetizationNetwork = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    private fun BannerSize.asApplovinAdSize() = when (this) {
        BannerSize.Banner -> AppLovinAdSize.BANNER
        BannerSize.Adaptive -> AppLovinAdSize.BANNER
        BannerSize.LeaderBoard -> AppLovinAdSize.LEADER
        BannerSize.MRec -> AppLovinAdSize.MREC
    }
}

private const val Tag = "ApplovinBanner"
private const val USD = "USD"
