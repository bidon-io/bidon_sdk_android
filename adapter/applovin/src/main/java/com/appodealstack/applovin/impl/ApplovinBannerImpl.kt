package com.appodealstack.applovin.impl

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinBannerAuctionParams
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInternal
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
    private val appLovinSdk: AppLovinSdk
) : AdSource.Banner<ApplovinBannerAuctionParams> {

    private var adView: AppLovinAdView? = null
    private var appLovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                appLovinAd = ad
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            priceFloor = lineItem?.priceFloor ?: 0.0,
                            adSource = this@ApplovinBannerImpl
                        )
                    )
                )
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logError(Tag, "Failed to receive ad. errorCode=$errorCode")
                adState.tryEmit(AdState.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
    }

    private val listener by lazy {
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                adState.tryEmit(AdState.Impression(ad.asAd()))
            }

            override fun adHidden(ad: AppLovinAd) {
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = appLovinAd?.asAd() ?: adView?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
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
    ): AdAuctionParams {
        val lineItem = lineItems.minByOrNull { it.priceFloor }
            ?.also(onLineItemConsumed)
        return ApplovinBannerAuctionParams(
            context = adContainer.context,
            lineItem = requireNotNull(lineItem),
            adaptiveBannerHeight = null,
            bannerSize = bannerSize
        )
    }

    override suspend fun bid(
        adParams: ApplovinBannerAuctionParams
    ): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams")
        lineItem = adParams.lineItem
        val adSize = adParams.bannerSize.asAppLovinAdSize() ?: error(BidonError.AdFormatIsNotSupported(demandId.demandId, adParams.bannerSize))
        val bannerView = AppLovinAdView(appLovinSdk, adSize, adParams.lineItem.adUnitId, adParams.context).also {
            adView = it
        }

        // TODO load and display
        bannerView.setAdLoadListener(requestListener)
        bannerView.loadNextAd()

        // TODO load but not display. Impossible to use adSize and zoneId simultaneously.
        // appLovinSdk.adService.loadNextAd(adSize, requestListener)

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
        requireNotNull(appLovinAd?.asAd()).also {
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
            currencyCode = USD
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
            currencyCode = USD
        )
    }

    private fun BannerSize.asAppLovinAdSize() = when (this) {
        BannerSize.Banner -> AppLovinAdSize.BANNER
        BannerSize.LeaderBoard -> AppLovinAdSize.LEADER
        BannerSize.MRec -> AppLovinAdSize.MREC
        BannerSize.Large -> null
        BannerSize.Adaptive -> null
    }
}

private const val Tag = "ApplovinMax Interstitial"
private const val USD = "USD"
