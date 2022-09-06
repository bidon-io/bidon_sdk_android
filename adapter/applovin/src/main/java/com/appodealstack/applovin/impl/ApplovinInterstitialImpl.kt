package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.ApplovinFullscreenAdAuctionParams
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.analytics.StatisticsCollector
import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.analytics.domain.StatisticsCollectorImpl
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.auctions.data.models.minByPricefloorOrNull
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * I have no idea how it works. There is no documentation.
 *
 * https://appodeal.slack.com/archives/C02PE4GAFU0/p1661421318406689
 */
internal class ApplovinInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val appLovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Interstitial<ApplovinFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var interstitialAd: AppLovinIncentivizedInterstitial? = null
    private var appLovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val requestListener by lazy {
        object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInternal(Tag, "adReceived: $this")
                appLovinAd = ad
                markBidFinished(
                    ecpm = requireNotNull(lineItem?.priceFloor),
                    roundStatus = RoundStatus.SuccessfulBid,
                )
                adState.tryEmit(
                    AdState.Bid(
                        AuctionResult(
                            ecpm = lineItem?.priceFloor ?: 0.0,
                            adSource = this@ApplovinInterstitialImpl,
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
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

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
        get() = appLovinAd?.asAd() ?: interstitialAd?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
        interstitialAd = null
        appLovinAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, priceFloor)
            ?.also(onLineItemConsumed)
        ApplovinFullscreenAdAuctionParams(
            activity = activity,
            lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
            timeoutMs = timeout,
        )
    }

    override suspend fun bid(
        adParams: ApplovinFullscreenAdAuctionParams
    ): AuctionResult {
        logInternal(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
        val incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(adParams.lineItem.adUnitId, appLovinSdk).also {
            interstitialAd = it
        }
        incentivizedInterstitial.preload(requestListener)
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
        requireNotNull(appLovinAd?.asAd()).also {
            adState.tryEmit(AdState.Fill(it))
        }
    }

    override fun show(activity: Activity) {
        logInternal(Tag, "Starting show: $this")
        val appLovinAd = appLovinAd
        if (interstitialAd?.isAdReadyToDisplay == true && appLovinAd != null) {
            interstitialAd?.show(appLovinAd, activity, null, listener, listener, listener)
        } else {
            adState.tryEmit(AdState.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinIncentivizedInterstitial?.asAd(): Ad {
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
}

private const val Tag = "Applovin Interstitial"
private const val USD = "USD"
