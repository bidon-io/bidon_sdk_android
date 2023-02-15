package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.ApplovinFullscreenAdAuctionParams
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.AdState
import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.BidonError
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.bidon.ads.DemandId
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
internal class ApplovinRewardedImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val applovinSdk: AppLovinSdk,
    private val auctionId: String
) : AdSource.Rewarded<ApplovinFullscreenAdAuctionParams>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId
    ) {

    private var rewardedAd: AppLovinIncentivizedInterstitial? = null
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
                            adSource = this@ApplovinRewardedImpl,
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
        object :
            AppLovinAdRewardListener,
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(Tag, "adDisplayed: $this")
                adState.tryEmit(AdState.Impression(ad.asAd()))
                adState.tryEmit(AdState.PaidRevenue(ad.asAd()))
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $this")
                adState.tryEmit(AdState.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $this")
                adState.tryEmit(AdState.Clicked(ad.asAd()))
            }

            override fun userRewardVerified(ad: AppLovinAd, response: MutableMap<String, String>?) {
                logInfo(Tag, "userRewardVerified: $this")
                adState.tryEmit(AdState.OnReward(ad.asAd(), reward = null))
            }

            override fun userOverQuota(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun userRewardRejected(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun validationRequestFailed(ad: AppLovinAd?, errorCode: Int) {}
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        rewardedAd = null
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

    override suspend fun bid(adParams: ApplovinFullscreenAdAuctionParams): AuctionResult {
        logInfo(Tag, "Starting with $adParams: $this")
        markBidStarted(adParams.lineItem.adUnitId)
        lineItem = adParams.lineItem
        val incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(adParams.lineItem.adUnitId, applovinSdk).also {
            rewardedAd = it
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
        logInfo(Tag, "Starting fill: $this")
        markFillStarted()
        requireNotNull(applovinAd?.asAd()).also {
            markFillFinished(RoundStatus.Successful)
            adState.tryEmit(AdState.Fill(it))
        }
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        val appLovinAd = applovinAd
        if (rewardedAd?.isAdReadyToDisplay == true && appLovinAd != null) {
            rewardedAd?.show(appLovinAd, activity, listener, listener, listener, listener)
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

private const val Tag = "Applovin Rewarded"
private const val USD = "USD"
