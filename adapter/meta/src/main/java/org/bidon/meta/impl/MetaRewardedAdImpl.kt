package org.bidon.meta.impl

import android.app.Activity
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.RewardedVideoAd
import com.facebook.ads.RewardedVideoAdListener
import org.bidon.meta.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
class MetaRewardedAdImpl :
    AdSource.Rewarded<MetaFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedVideoAd: RewardedVideoAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedVideoAd?.isAdLoaded ?: false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MetaFullscreenAuctionParams(
                context = activity.applicationContext,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: MetaFullscreenAuctionParams) {
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            adParams.payload ?: run {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                    )
                )
                return
            }
        }
        val rewardedAd = RewardedVideoAd(adParams.context, adParams.placementId).also {
            rewardedVideoAd = it
        }
        rewardedAd.loadAd(
            rewardedAd.buildLoadAdConfig()
                .withAdListener(object : RewardedVideoAdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        logInfo(TAG, "Error while loading ad(${adError?.errorCode}: ${adError?.errorMessage}). $this")
                        emitEvent(AdEvent.LoadFailed(adError.asBidonError()))
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        logInfo(TAG, "onAdLoaded $ad: $rewardedVideoAd, $this")
                        val bidonAd = getAd()
                        if (rewardedVideoAd != null && bidonAd != null) {
                            emitEvent(AdEvent.Fill(bidonAd))
                        } else {
                            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                        }
                    }

                    override fun onAdClicked(ad: Ad?) {
                        logInfo(TAG, "onAdClicked: $this")
                        val bidonAd = getAd() ?: return
                        emitEvent(AdEvent.Clicked(bidonAd))
                    }

                    override fun onLoggingImpression(ad: Ad?) {
                        logInfo(TAG, "onAdImpression: $this")
                        val bidonAd = getAd() ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = bidonAd,
                                adValue = AdValue(
                                    adRevenue = adParams.price / 1000.0,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }

                    override fun onRewardedVideoCompleted() {
                        logInfo(TAG, "onRewardedVideoCompleted")
                        val bidonAd = getAd() ?: return
                        emitEvent(AdEvent.Shown(bidonAd))
                        emitEvent(AdEvent.OnReward(bidonAd, null))
                    }

                    override fun onRewardedVideoClosed() {
                        logInfo(TAG, "onRewardedVideoClosed")
                        val bidonAd = getAd() ?: return
                        emitEvent(AdEvent.Closed(bidonAd))
                        this@MetaRewardedAdImpl.rewardedVideoAd = null
                    }
                })
                .withBid(adParams.payload)
                .build()
        )
    }

    override fun destroy() {
        rewardedVideoAd?.destroy()
        rewardedVideoAd = null
    }

    override fun show(activity: Activity) {
        val rewardedAd = rewardedVideoAd
        if (rewardedAd != null && rewardedAd.isAdLoaded) {
            rewardedAd.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "MetaRewardedAdImpl"
