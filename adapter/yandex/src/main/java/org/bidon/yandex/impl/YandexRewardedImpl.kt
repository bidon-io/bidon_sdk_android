package org.bidon.yandex.impl

import android.app.Activity
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.yandex.ext.asBidonAdValue
import org.bidon.yandex.ext.asBidonError

internal class YandexRewardedImpl :
    AdSource.Rewarded<YandexFullscreenAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl(),
    YandexLoader by singleLoader {

    private var rewardedAd: RewardedAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            YandexFullscreenAuctionParam(
                context = activity.applicationContext,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: YandexFullscreenAuctionParam) {
        val adUnitId = adParams.adUnitId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")))

        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        val adLoadListener = object : RewardedAdLoadListener {
            override fun onAdLoaded(rewarded: RewardedAd) {
                this@YandexRewardedImpl.rewardedAd = rewarded
                logInfo(TAG, "onAdLoaded: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Fill(ad))
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                logInfo(TAG, "onAdFailedToLoad: ${error.code} ${error.description}. $this")
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }
        }
        requestRewardedAd(adParams.context, adRequestConfiguration, adLoadListener)
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            rewardedAd?.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {
                    logInfo(TAG, "onAdShown: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Shown(ad))
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onAdDismissed() {
                    logInfo(TAG, "onAdDismissed: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Closed(ad))
                }

                override fun onAdFailedToShow(adError: AdError) {
                    logInfo(TAG, "onAdFailedToShow: ${adError.description}. $this")
                    emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                }

                override fun onAdImpression(impressionData: ImpressionData?) {
                    logInfo(TAG, "onAdImpression: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.PaidRevenue(ad = ad, adValue = impressionData.asBidonAdValue()))
                }

                override fun onRewarded(reward: Reward) {
                    logInfo(TAG, "onRewarded: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.OnReward(ad = ad, reward = null))
                }
            })
            rewardedAd?.show(activity)
        }
    }

    override fun destroy() {
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
    }
}

private const val TAG = "YandexRewardedImpl"