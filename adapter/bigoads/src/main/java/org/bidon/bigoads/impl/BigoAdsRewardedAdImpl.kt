package org.bidon.bigoads.impl

import android.app.Activity
import android.content.Context
import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import sg.bigo.ads.BigoAdSdk
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
    AdSource.Rewarded<BigoFullscreenAuctionParams>,
    AdLoadingType.Bidding<BigoFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardVideoAd: RewardVideoAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardVideoAd != null && rewardVideoAd?.isExpired != false

    override fun destroy() {
        rewardVideoAd?.destroy()
        rewardVideoAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoBannerAuctionParams(
                bannerFormat = bannerFormat,
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for BigoAds"
                },
                slotId = requireNotNull(json?.optString("slot_id")) {
                    "Slot id is required for BigoAds"
                },
                bidPrice = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for BigoAds"
                },
            )
        }
    }

    override fun getToken(context: Context): String? = BigoAdSdk.getBidderToken()

    override fun adRequest(adParams: BigoFullscreenAuctionParams) {
        val builder = RewardVideoAdRequest.Builder()
        builder
            .withBid(adParams.payload)
            .withSlotId(adParams.slotId)
        val loader = RewardVideoAdLoader.Builder().withAdLoadListener(object : AdLoadListener<RewardVideoAd> {
            override fun onError(adError: AdError) {
                val error = adError.asBidonError()
                logError(TAG, "Error while loading ad: $adError. $this", error)
                emitEvent(AdEvent.LoadFailed(error))
            }

            override fun onAdLoaded(rewardVideoAd: RewardVideoAd) {
                logInfo(TAG, "onAdLoaded: $rewardVideoAd, $this")
                this@BigoAdsRewardedAdImpl.rewardVideoAd = rewardVideoAd
                rewardVideoAd.setAdInteractionListener(object : RewardAdInteractionListener {
                    override fun onAdError(error: AdError) {
                        val cause = error.asBidonError()
                        logError(TAG, "onAdError: $this", cause)
                        emitEvent(AdEvent.ShowFailed(cause))
                    }

                    override fun onAdImpression() {
                        logInfo(TAG, "onAdImpression: $this")
                        val ad = getAd(this@BigoAdsRewardedAdImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParams.bidPrice,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }

                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        val ad = getAd(this@BigoAdsRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdOpened() {
                        logInfo(TAG, "onAdOpened: $this")
                        val ad = getAd(this@BigoAdsRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Shown(ad))
                    }

                    override fun onAdClosed() {
                        logInfo(TAG, "onAdClosed: $this")
                        val ad = getAd(this@BigoAdsRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }

                    override fun onAdRewarded() {
                        logInfo(TAG, "onAdRewarded: $this")
                        val ad = getAd(this@BigoAdsRewardedAdImpl) ?: return
                        emitEvent(AdEvent.OnReward(ad, null))
                    }
                })
                emitEvent(
                    AdEvent.Bid(
                        AuctionResult.Bidding(
                            adSource = this@BigoAdsRewardedAdImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }
        })
        loader.build()
            .loadAd(builder.build())
    }

    override fun show(activity: Activity) {
        val rewardVideoAd = rewardVideoAd
        if (rewardVideoAd == null) {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        } else {
            rewardVideoAd.show()
        }
    }

    override fun fill() {
        val ad = getAd(this)
        if (rewardVideoAd != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }
}

private const val TAG = "BigoAdsRewardedAd"