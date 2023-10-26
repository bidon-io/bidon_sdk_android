package org.bidon.inmobi.impl

import android.app.Activity
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
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
    StatisticsCollector by StatisticsCollectorImpl(),
    Mode.Network {

    private var rewardedAd: InMobiInterstitial? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.isReady() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            InmobiFullscreenAuctionParams(
                context = activity.applicationContext,
                lineItem = lineItem,
                price = lineItem.pricefloor,
            )
        }
    }

    override fun load(adParams: InmobiFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        val interstitialAd = InMobiInterstitial(
            adParams.context, adParams.placementId,
            object : InterstitialAdEventListener() {
                override fun onAdLoadSucceeded(interstitial: InMobiInterstitial, adMetaInfo: AdMetaInfo) {
                    logInfo(TAG, "onAdLoadSucceeded: $this, ${adMetaInfo.bid} USD")
                    setPrice(adMetaInfo.bid)
                    emitEvent(AdEvent.Fill(getAd() ?: return))
                }

                override fun onAdLoadFailed(interstitial: InMobiInterstitial, status: InMobiAdRequestStatus) {
                    logError(
                        tag = TAG,
                        message = "Error while loading ad: ${status.statusCode} ${status.message}. $this",
                        error = BidonError.Unspecified(demandId)
                    )
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
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
                    logError(TAG, "onAdDisplayFailed. $this", BidonError.Unspecified(demandId))
                    emitEvent(AdEvent.ShowFailed(BidonError.Unspecified(demandId)))
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
