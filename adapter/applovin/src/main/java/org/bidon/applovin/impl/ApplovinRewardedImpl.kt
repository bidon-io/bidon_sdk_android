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
import org.bidon.applovin.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
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
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: AppLovinIncentivizedInterstitial? = null
    private var applovinAd: AppLovinAd? = null
    private var adUnit: AdUnit? = null

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
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(AdEvent.PaidRevenue(it, adUnit?.pricefloor.asBidonAdValue()))
                }
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Closed(it))
                }
                destroy()
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Clicked(it))
                }
            }

            override fun userRewardVerified(ad: AppLovinAd, response: MutableMap<String, String>?) {
                logInfo(TAG, "userRewardVerified: $this")
                getAd()?.let {
                    emitEvent(AdEvent.OnReward(it, reward = null))
                }
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
        get() = rewardedAd?.isAdReadyToDisplay == true

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        rewardedAd = null
        applovinAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ApplovinFullscreenAdAuctionParams(
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: ApplovinFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adUnit = adParams.adUnit
        val zoneId = adParams.zoneId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "zoneId")
                )
            )
            return
        }
        val incentivizedInterstitial =
            AppLovinIncentivizedInterstitial.create(zoneId, applovinSdk).also {
                rewardedAd = it
            }
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                applovinAd = ad
                getAd()?.let {
                    emitEvent(AdEvent.Fill(it))
                }
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(errorCode.asBidonError()))
            }
        }
        logInfo(TAG, "Starting fill: $this")
        incentivizedInterstitial.preload(requestListener)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val applovinAd = this.applovinAd
        if (rewardedAd?.isAdReadyToDisplay == true && applovinAd != null) {
            rewardedAd?.show(applovinAd, activity.applicationContext, listener, listener, listener, listener)
            this.applovinAd = null
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "Applovin Rewarded"
