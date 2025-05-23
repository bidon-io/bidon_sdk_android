package org.bidon.inmobi.impl

import android.app.Activity
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import org.bidon.inmobi.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal class InmobiRewardedImpl :
    AdSource.Rewarded<InmobiFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: InMobiInterstitial? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.isReady() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val adUnit = adUnit
            InmobiFullscreenAuctionParams(
                context = activity.applicationContext,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: InmobiFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        val interstitialAd = InMobiInterstitial(
            adParams.context, adParams.placementId,
            object : InterstitialAdEventListener() {
                override fun onAdLoadSucceeded(interstitial: InMobiInterstitial, adMetaInfo: AdMetaInfo) {
                    logInfo(TAG, "onAdLoadSucceeded: $this, ${adMetaInfo.bid} USD")
                    setPrice(adMetaInfo.bid)
                    emitEvent(AdEvent.Fill(getAd() ?: return))
                }

                override fun onAdLoadFailed(interstitial: InMobiInterstitial, status: InMobiAdRequestStatus) {
                    logInfo(TAG, "Error while loading ad: ${status.statusCode} ${status.message}. $this")
                    emitEvent(AdEvent.LoadFailed(status.asBidonError()))
                }

                override fun onAdClicked(interstitial: InMobiInterstitial, map: MutableMap<Any, Any>?) {
                    logInfo(TAG, "onAdClicked: $map, $this")
                    emitEvent(AdEvent.Clicked(getAd() ?: return))
                }

                override fun onAdDisplayed(interstitial: InMobiInterstitial, adMetaInfo: AdMetaInfo) {
                    logInfo(TAG, "onAdImpression: $this")
                    val ad = getAd() ?: return
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = adMetaInfo.bid / 1000.0,
                                precision = Precision.Precise,
                                currency = AdValue.USD,
                            )
                        )
                    )
                    logInfo(TAG, "onAdDisplayed: $this")
                    emitEvent(AdEvent.Shown(ad))
                }

                override fun onAdDisplayFailed(interstitial: InMobiInterstitial) {
                    val error = BidonError.Unspecified(demandId)
                    logError(TAG, "onAdDisplayFailed. $this", error)
                    emitEvent(AdEvent.ShowFailed(error))
                }

                override fun onAdDismissed(interstitial: InMobiInterstitial) {
                    logInfo(TAG, "onAdClosed: $this")
                    emitEvent(AdEvent.Closed(getAd() ?: return))
                    this@InmobiRewardedImpl.rewardedAd = null
                }

                override fun onRewardsUnlocked(interstitial: InMobiInterstitial, rewards: MutableMap<Any, Any>?) {
                    logInfo(TAG, "onAdRewarded: $rewards, $this")
                    emitEvent(AdEvent.OnReward(getAd() ?: return, null))
                }
            }
        )
        this.rewardedAd = interstitialAd
        interstitialAd.load()
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (isAdReadyToShow) {
            rewardedAd?.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy")
        rewardedAd = null
    }
}

private const val TAG = "InmobiRewardedImpl"
