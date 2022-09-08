package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinBannerAuctionParams
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.auction.minByPricefloorOrNull
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.adapter.AdState
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.*
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.impl.StatisticsCollectorImpl
import com.appodealstack.bidon.domain.stats.impl.logInternal
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
    private val appLovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Banner<ApplovinBannerAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var adView: AppLovinAdView? = null
    private var appLovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInternal(Tag, "adReceived: $this")
                appLovinAd = ad
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
                logInternal(Tag, "failedToReceiveAd: errorCode=$errorCode. $this")
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
                logInternal(Tag, "adDisplayed: $this")
                adState.tryEmit(AdState.Impression(ad.asAd()))
            }

            override fun adHidden(ad: AppLovinAd) {
                logInternal(Tag, "adHidden: $this")
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInternal(Tag, "adClicked: $this")
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = appLovinAd?.asAd() ?: adView?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy $this")
        adView = null
        appLovinAd = null
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
        logInternal(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
        val adSize = adParams.bannerSize.asAppLovinAdSize() ?: error(
            BidonError.AdFormatIsNotSupported(
                demandId.demandId,
                adParams.bannerSize
            )
        )
        val bannerView = AppLovinAdView(appLovinSdk, adSize, adParams.lineItem.adUnitId, adParams.context).also {
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
        logInternal(Tag, "Starting fill: $this")
        markFillStarted()
        requireNotNull(appLovinAd?.asAd()).also {
            markFillFinished(RoundStatus.Successful)
            adState.tryEmit(AdState.Fill(it))
        }
    }

    override fun show(activity: Activity) {
    }

    override fun getAdView(): View = requireNotNull(adView)

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

    private fun BannerSize.asAppLovinAdSize() = when (this) {
        BannerSize.Banner -> AppLovinAdSize.BANNER
        BannerSize.Adaptive -> AppLovinAdSize.BANNER
        BannerSize.LeaderBoard -> AppLovinAdSize.LEADER
        BannerSize.MRec -> AppLovinAdSize.MREC
    }
}

private const val Tag = "Applovin Banner"
private const val USD = "USD"
