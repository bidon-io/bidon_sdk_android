package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinFullscreenAdAuctionParams
import com.appodealstack.bidon.adapter.*
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.minByPricefloorOrNull
import com.appodealstack.bidon.config.BidonError
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
internal class ApplovinInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val applovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Interstitial<ApplovinFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var interstitialAd: AppLovinIncentivizedInterstitial? = null
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
                            adSource = this@ApplovinInterstitialImpl,
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
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $this")
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
                adEvent.tryEmit(AdEvent.PaidRevenue(ad.asAd()))
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $this")
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: interstitialAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy")
        interstitialAd = null
        applovinAd = null
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
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
        val incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(adParams.lineItem.adUnitId, applovinSdk).also {
            interstitialAd = it
        }
        incentivizedInterstitial.preload(requestListener)
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
        logInfo(Tag, "Starting show: $this")
        val appLovinAd = applovinAd
        if (interstitialAd?.isAdReadyToDisplay == true && appLovinAd != null) {
            interstitialAd?.show(appLovinAd, activity, null, listener, listener, listener)
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinIncentivizedInterstitial?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            price = lineItem?.priceFloor ?: 0.0,
            sourceAd = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = USD,
            auctionId = auctionId,
        )
    }
}

private const val Tag = "ApplovinInterstitial"
private const val USD = "USD"
