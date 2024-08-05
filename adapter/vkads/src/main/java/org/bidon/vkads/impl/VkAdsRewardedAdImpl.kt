package org.bidon.vkads.impl

import android.app.Activity
import com.my.target.ads.Reward
import com.my.target.ads.RewardedAd
import com.my.target.common.models.IAdLoadingError
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
import org.bidon.vkads.ext.asBidonError

internal class VkAdsRewardedAdImpl :
    AdSource.Rewarded<VkAdsFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: RewardedAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VkAdsFullscreenAuctionParams(
                activity = activity,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: VkAdsFullscreenAuctionParams) {
        val slotId = adParams.slotId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "slotId")))

        val rewardedAd = RewardedAd(slotId, adParams.activity)
            .also { rewardedAd = it }
        rewardedAd.customParams.setCustomParam("mediation", adParams.mediation)
        rewardedAd.listener = object : RewardedAd.RewardedAdListener {
            override fun onLoad(rewarded: RewardedAd) {
                logInfo(TAG, "onLoad: $this")
                emitEvent(AdEvent.Fill(getAd() ?: return))
            }

            override fun onNoAd(error: IAdLoadingError, rewarded: RewardedAd) {
                logInfo(TAG, "onNoAd: ${error.code} ${error.message}. $this")
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }

            override fun onClick(rewarded: RewardedAd) {
                logInfo(TAG, "onClick: $this")
                emitEvent(AdEvent.Clicked(getAd() ?: return))
            }

            override fun onDismiss(rewarded: RewardedAd) {
                logInfo(TAG, "onDismiss: $this")
                emitEvent(AdEvent.Closed(getAd() ?: return))
            }

            override fun onReward(reward: Reward, rewarded: RewardedAd) {
                logInfo(TAG, "onAdRewarded: $reward, $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.OnReward(ad = ad, reward = null))
                }
            }

            override fun onDisplay(rewarded: RewardedAd) {
                logInfo(TAG, "onDisplay: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = AdValue(
                                adRevenue = adParams.price,
                                currency = AdValue.USD,
                                precision = Precision.Precise
                            )
                        )
                    )
                }
            }
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            val bidId = adParams.bidId
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "bidId")))
            rewardedAd.loadFromBid(bidId)
        } else {
            rewardedAd.load()
        }
    }

    override fun show(activity: Activity) {
        val rewardedAd = rewardedAd
        if (rewardedAd != null) {
            rewardedAd.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        rewardedAd?.destroy()
        rewardedAd = null
    }
}

private const val TAG = "VkAdsRewardedImpl"
