package org.bidon.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.bidon.applovin.ApplovinFullscreenAdAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

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
                    ecpm = requireNotNull(lineItem?.pricefloor),
                    roundStatus = RoundStatus.Successful,
                )
                adEvent.tryEmit(
                    AdEvent.Bid(
                        AuctionResult(
                            ecpm = lineItem?.pricefloor ?: 0.0,
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
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
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
                adEvent.tryEmit(AdEvent.Shown(ad.asAd()))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = lineItem?.pricefloor.asBidonAdValue()
                    )
                )
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(Tag, "adHidden: $this")
                adEvent.tryEmit(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(Tag, "adClicked: $this")
                adEvent.tryEmit(AdEvent.Clicked(ad.asAd()))
            }

            override fun userRewardVerified(ad: AppLovinAd, response: MutableMap<String, String>?) {
                logInfo(Tag, "userRewardVerified: $this")
                adEvent.tryEmit(AdEvent.OnReward(ad.asAd(), reward = null))
            }

            override fun userOverQuota(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun userRewardRejected(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun validationRequestFailed(ad: AppLovinAd?, errorCode: Int) {}
        }
    }

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE)
    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override val ad: Ad?
        get() = applovinAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        rewardedAd = null
        applovinAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        pricefloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): Result<AdAuctionParams> = runCatching {
        val lineItem = lineItems
            .minByPricefloorOrNull(demandId, pricefloor)
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
        if (rewardedAd?.isAdReadyToDisplay == true && appLovinAd != null) {
            rewardedAd?.show(appLovinAd, activity, listener, listener, listener, listener)
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinIncentivizedInterstitial?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = lineItem?.pricefloor ?: 0.0,
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
            ecpm = lineItem?.pricefloor ?: 0.0,
            sourceAd = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.DefaultCurrency,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }
}

private const val Tag = "Applovin Rewarded"
