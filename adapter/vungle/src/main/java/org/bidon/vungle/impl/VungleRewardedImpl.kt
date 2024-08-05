package org.bidon.vungle.impl

import android.app.Activity
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.RewardedAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleError
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
import org.bidon.sdk.stats.models.BidType
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleRewardedImpl :
    AdSource.Rewarded<VungleFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: RewardedAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.canPlayAd() == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleFullscreenAuctionParams(
                activity = activity,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: VungleFullscreenAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        val placementId = adParams.placementId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")))

        val rewardedAd = RewardedAd(adParams.activity, placementId, AdConfig())
            .also { rewardedAd = it }
        rewardedAd.adListener = object : RewardedAdListener {
            override fun onAdLoaded(baseAd: BaseAd) {
                val ad = getAd()
                if (ad != null) {
                    emitEvent(AdEvent.Fill(ad))
                } else {
                    emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                }
            }

            override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onError placementId=${baseAd.placementId}. $this", null)
                emitEvent(AdEvent.LoadFailed(adError.asBidonError()))
            }

            override fun onAdClicked(baseAd: BaseAd) {
                logInfo(TAG, "onAdClick: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdEnd(baseAd: BaseAd) {
                logInfo(TAG, "onAdEnd: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Closed(ad))
            }

            override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                logError(TAG, "onAdError: $this", adError)
                emitEvent(AdEvent.ShowFailed(adError.asBidonError()))
            }

            override fun onAdRewarded(baseAd: BaseAd) {
                logInfo(TAG, "onAdRewarded: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.OnReward(ad, null))
            }

            override fun onAdStart(baseAd: BaseAd) {
                logInfo(TAG, "onAdStart: $this")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Shown(ad))
            }

            override fun onAdLeftApplication(baseAd: BaseAd) {
                logInfo(TAG, "onAdViewed: $this")
                val ad = getAd() ?: return
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = adParams.price / 1000.0,
                            precision = Precision.Precise,
                            currency = AdValue.USD,
                        )
                    )
                )
            }

            override fun onAdImpression(baseAd: BaseAd) {}
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            rewardedAd.load(payload)
        } else {
            rewardedAd.load(adParams.payload)
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            rewardedAd?.play()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        rewardedAd?.adListener = null
        rewardedAd = null
    }
}

private const val TAG = "VungleRewardedImpl"