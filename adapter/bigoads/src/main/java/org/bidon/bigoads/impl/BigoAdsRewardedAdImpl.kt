package org.bidon.bigoads.impl

import android.app.Activity
import org.bidon.bigoads.ext.asBidonError
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
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.RewardAdInteractionListener
import sg.bigo.ads.api.RewardVideoAd
import sg.bigo.ads.api.RewardVideoAdLoader
import sg.bigo.ads.api.RewardVideoAdRequest

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal class BigoAdsRewardedAdImpl :
    AdSource.Rewarded<BigoAdsFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardVideoAd: RewardVideoAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardVideoAd?.isExpired == false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoAdsFullscreenAuctionParams(adUnit = adUnit)
        }
    }

    override fun load(adParams: BigoAdsFullscreenAuctionParams) {
        val slotId = adParams.slotId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "slotId")))

        val loader = RewardVideoAdLoader.Builder()
            .withAdLoadListener(object : AdLoadListener<RewardVideoAd> {
                override fun onError(adError: AdError) {
                    val error = adError.asBidonError()
                    logError(TAG, "Error while loading ad: ${adError.code} ${adError.message}. $this", error)
                    emitEvent(AdEvent.LoadFailed(error))
                }

                override fun onAdLoaded(rewardVideoAd: RewardVideoAd) {
                    logInfo(TAG, "onAdLoaded: $rewardVideoAd, $this")
                    this@BigoAdsRewardedAdImpl.rewardVideoAd = rewardVideoAd
                    fillAd(rewardVideoAd, adParams)
                }
            })
            .build()

        val adRequestBuilder = RewardVideoAdRequest.Builder()
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            adRequestBuilder.withBid(payload)
        }
        adRequestBuilder.withSlotId(slotId)
        loader.loadAd(adRequestBuilder.build())
    }

    override fun show(activity: Activity) {
        val rewardVideoAd = rewardVideoAd
        if (rewardVideoAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        } else {
            rewardVideoAd.show()
        }
    }

    override fun destroy() {
        rewardVideoAd?.destroy()
        rewardVideoAd = null
    }

    private fun fillAd(
        rewardVideoAd: RewardVideoAd,
        adParams: BigoAdsFullscreenAuctionParams
    ) {
        rewardVideoAd.setAdInteractionListener(object : RewardAdInteractionListener {
            override fun onAdError(error: AdError) {
                val cause = error.asBidonError()
                logError(TAG, "onAdError: $this", cause)
                emitEvent(AdEvent.ShowFailed(cause))
            }

            override fun onAdImpression() {
                logInfo(TAG, "onAdImpression: $this")
                getAd()?.let { ad ->
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
            }

            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Clicked(ad))
                }
            }

            override fun onAdOpened() {
                logInfo(TAG, "onAdOpened: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Shown(ad))
                }
            }

            override fun onAdClosed() {
                logInfo(TAG, "onAdClosed: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Closed(ad))
                }
                this@BigoAdsRewardedAdImpl.rewardVideoAd = null
            }

            override fun onAdRewarded() {
                logInfo(TAG, "onAdRewarded: $this")
                getAd()?.let { ad ->
                    emitEvent(AdEvent.OnReward(ad, null))
                }
            }
        })
        getAd()?.let { ad ->
            emitEvent(AdEvent.Fill(ad))
        }
    }
}

private const val TAG = "BigoAdsRewardedAd"