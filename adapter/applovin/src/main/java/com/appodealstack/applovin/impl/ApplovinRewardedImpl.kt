package com.appodealstack.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.*
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.applovin.ApplovinFullscreenAdAuctionParams
import com.appodealstack.bidon.adapters.*
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
internal class ApplovinRewardedImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val appLovinSdk: AppLovinSdk
) : AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {

    private var rewardedAd: AppLovinIncentivizedInterstitial? = null
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
                            adSource = this@ApplovinRewardedImpl
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
            AppLovinAdRewardListener,
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

            override fun userRewardVerified(ad: AppLovinAd, response: MutableMap<String, String>?) {
                adState.tryEmit(AdState.OnReward(ad.asAd(), reward = null))
            }

            override fun userOverQuota(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun userRewardRejected(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun validationRequestFailed(ad: AppLovinAd?, errorCode: Int) {}
        }
    }

    override val adState = MutableSharedFlow<AdState>(extraBufferCapacity = Int.MAX_VALUE)

    override val ad: Ad?
        get() = appLovinAd?.asAd() ?: rewardedAd?.asAd()

    override fun destroy() {
        logInternal(Tag, "destroy")
        rewardedAd = null
        appLovinAd = null
    }

    override fun getAuctionParams(
        activity: Activity,
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        onLineItemConsumed: (LineItem) -> Unit,
    ): AdAuctionParams {
        val lineItem = lineItems.minByOrNull { it.priceFloor }
            ?.also(onLineItemConsumed)
        return ApplovinFullscreenAdAuctionParams(
            activity = activity,
            lineItem = requireNotNull(lineItem),
            timeoutMs = timeout,
        )
    }

    override suspend fun bid(
        adParams: ApplovinFullscreenAdAuctionParams
    ): Result<AuctionResult> {
        logInternal(Tag, "Starting with $adParams")
        lineItem = adParams.lineItem
        val incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(adParams.lineItem.adUnitId, appLovinSdk).also {
            rewardedAd = it
        }
        incentivizedInterstitial.preload(requestListener)
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
        val appLovinAd = appLovinAd
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
}

private const val Tag = "ApplovinMax Interstitial"
private const val USD = "USD"