package org.bidon.meta.impl

import android.app.Activity
import android.content.Context
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.BidderTokenProvider
import com.facebook.ads.RewardedVideoAd
import com.facebook.ads.RewardedVideoAdListener
import org.bidon.meta.ext.asBidonError
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

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
class MetaRewardedAdImpl :
    AdSource.Rewarded<MetaFullscreenAuctionParams>,
    AdLoadingType.Bidding<MetaFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MetaFullscreenAuctionParams? = null
    private var rewardedVideoAd: RewardedVideoAd? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedVideoAd?.isAdLoaded ?: false

    override fun getToken(context: Context): String? {
        return BidderTokenProvider.getBidderToken(context)
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MetaFullscreenAuctionParams(
                context = activity.applicationContext,
                placementId = requireNotNull(json?.optString("placement_id")) {
                    "Placement id is required for Meta"
                },
                price = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for Meta"
                },
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for Meta"
                },
            )
        }
    }

    override fun adRequest(adParams: MetaFullscreenAuctionParams) {
        this.adParams = adParams
        val rewardedAd = RewardedVideoAd(adParams.context, adParams.placementId).also {
            rewardedVideoAd = it
        }
        rewardedAd.loadAd(
            rewardedAd.buildLoadAdConfig()
                .withAdListener(object : RewardedVideoAdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        val error = adError.asBidonError()
                        logError(
                            TAG,
                            "Error while loading ad: AdError(${adError?.errorCode}: ${adError?.errorMessage}). $this",
                            error
                        )
                        emitEvent(AdEvent.LoadFailed(error))
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        logInfo(TAG, "onAdLoaded $ad: $rewardedVideoAd, $this")
                        emitEvent(
                            AdEvent.Bid(
                                AuctionResult.Bidding(
                                    adSource = this@MetaRewardedAdImpl,
                                    roundStatus = RoundStatus.Successful
                                )
                            )
                        )
                    }

                    override fun onAdClicked(ad: Ad?) {
                        logInfo(TAG, "onAdClicked: $this")
                        val bidonAd = getAd(this@MetaRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Clicked(bidonAd))
                    }

                    override fun onLoggingImpression(ad: Ad?) {
                        logInfo(TAG, "onAdImpression: $this")
                        val bidonAd = getAd(this@MetaRewardedAdImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = bidonAd,
                                adValue = AdValue(
                                    adRevenue = adParams.price,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }

                    override fun onRewardedVideoCompleted() {
                        logInfo(TAG, "onRewardedVideoCompleted")
                        val bidonAd = getAd(this@MetaRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Shown(bidonAd))
                        emitEvent(AdEvent.OnReward(bidonAd, null))
                    }

                    override fun onRewardedVideoClosed() {
                        logInfo(TAG, "onRewardedVideoClosed")
                        val bidonAd = getAd(this@MetaRewardedAdImpl) ?: return
                        emitEvent(AdEvent.Closed(bidonAd))
                    }
                })
                .withBid(adParams.payload)
                .build()
        )
    }

    override fun fill() {
        val ad = getAd(this)
        if (rewardedVideoAd != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }

    override fun destroy() {
        rewardedVideoAd?.destroy()
        rewardedVideoAd = null
        adParams = null
    }

    override fun show(activity: Activity) {
        val rewardedAd = rewardedVideoAd
        if (rewardedAd != null && rewardedAd.isAdLoaded) {
            rewardedAd.show()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }
}

private const val TAG = "MetaRewardedAdImpl"
