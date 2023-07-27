package org.bidon.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdRewardListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinSdk
import org.bidon.applovin.ApplovinFullscreenAdAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * I have no idea how it works. There is no documentation.
 *
 * https://appodeal.slack.com/archives/C02PE4GAFU0/p1661421318406689
 */
internal class ApplovinRewardedImpl(
    private val applovinSdk: AppLovinSdk,
) : AdSource.Rewarded<ApplovinFullscreenAdAuctionParams>,
    AdLoadingType.Network<ApplovinFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: AppLovinIncentivizedInterstitial? = null
    private var applovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val listener by lazy {
        object :
            AppLovinAdRewardListener,
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(
                ad: AppLovinAd,
                percentViewed: Double,
                fullyWatched: Boolean
            ) {
            }

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(TAG, "adDisplayed: $this")
                emitEvent(AdEvent.Shown(ad.asAd()))
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = lineItem?.pricefloor.asBidonAdValue()
                    )
                )
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $this")
                emitEvent(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $this")
                emitEvent(AdEvent.Clicked(ad.asAd()))
            }

            override fun userRewardVerified(ad: AppLovinAd, response: MutableMap<String, String>?) {
                logInfo(TAG, "userRewardVerified: $this")
                emitEvent(AdEvent.OnReward(ad.asAd(), reward = null))
                sendRewardImpression()
            }

            override fun userOverQuota(ad: AppLovinAd?, response: MutableMap<String, String>?) {}
            override fun userRewardRejected(
                ad: AppLovinAd?,
                response: MutableMap<String, String>?
            ) {
            }

            override fun validationRequestFailed(ad: AppLovinAd?, errorCode: Int) {}
        }
    }

    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        rewardedAd = null
        applovinAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = lineItems
                .minByPricefloorOrNull(demandId, pricefloor)
                ?.also(onLineItemConsumed)
            ApplovinFullscreenAdAuctionParams(
                lineItem = lineItem ?: error(BidonError.NoAppropriateAdUnitId),
                timeoutMs = timeout,
            )
        }
    }

    override fun fill(adParams: ApplovinFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        lineItem = adParams.lineItem
        val incentivizedInterstitial =
            AppLovinIncentivizedInterstitial.create(adParams.lineItem.adUnitId, applovinSdk).also {
                rewardedAd = it
            }
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                applovinAd = ad
                emitEvent(AdEvent.Fill(requireNotNull(applovinAd?.asAd())))
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        incentivizedInterstitial.preload(requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val appLovinAd = applovinAd
        if (rewardedAd?.isAdReadyToDisplay == true && appLovinAd != null) {
            rewardedAd?.show(appLovinAd, activity, listener, listener, listener, listener)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }
}

private const val TAG = "Applovin Rewarded"
