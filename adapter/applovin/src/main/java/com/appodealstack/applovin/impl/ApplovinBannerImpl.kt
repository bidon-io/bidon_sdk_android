package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinBannerAuctionParams
import com.appodealstack.applovin.ext.asBidonAdValue
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.config.BidonError
import com.appodealstack.bidon.logs.analytic.AdValue
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
                adEvent.tryEmit(
                    AdEvent.Bid(
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
                        adValue = lineItem?.priceFloor.asBidonAdValue()
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
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerFormat: BannerFormat,
        onLineItemConsumed: (LineItem) -> Unit
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
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
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
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
                    adSource = this
                )
            }
            is AdEvent.Bid -> state.result
            else -> error("unexpected: $state")
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
        requireNotNull(applovinAd?.asAd()).also {
            markFillFinished(RoundStatus.Successful)
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
            eCPM = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            eCPM = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
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
