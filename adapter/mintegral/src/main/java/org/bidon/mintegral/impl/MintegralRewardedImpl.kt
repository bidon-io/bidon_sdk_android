package org.bidon.mintegral.impl

import android.app.Activity
import com.mbridge.msdk.out.MBBidRewardVideoHandler
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import com.mbridge.msdk.out.RewardVideoListener
import org.bidon.mintegral.MintegralAuctionParam
import org.bidon.mintegral.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralRewardedImpl :
    AdSource.Rewarded<MintegralAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewardedAd: MBBidRewardVideoHandler? = null
    private val fillStarted = AtomicBoolean(false)

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.isBidReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralAuctionParam(
                activity = activity,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: MintegralAuctionParam) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        adParams.unitId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "unitId")
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
        val handler = MBBidRewardVideoHandler(
            adParams.activity.applicationContext,
            adParams.placementId,
            adParams.unitId
        ).also {
            rewardedAd = it
        }
        handler.setRewardVideoListener(object : RewardVideoListener {
            override fun onLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onLoadSuccess $mBridgeIds")
                if (!fillStarted.getAndSet(true)) {
                    fillAd()
                }
            }

            override fun onVideoLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoLoadSuccess $mBridgeIds")
                if (!fillStarted.getAndSet(true)) {
                    fillAd()
                }
            }

            override fun onVideoLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logInfo(TAG, "onVideoLoadFail $mBridgeIds")
                emitEvent(
                    AdEvent.LoadFailed(
                        message?.asBidonError()
                            ?: BidonError.NoFill(demandId)
                    )
                )
            }

            override fun onVideoAdClicked(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoAdClicked $mBridgeIds")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdShow(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdShow $mBridgeIds")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Shown(ad))
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = adParams.price / 1000.0,
                            precision = Precision.Precise,
                            currency = AdValue.USD
                        )
                    )
                )
            }

            override fun onAdClose(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
                logInfo(TAG, "onAdClose $mBridgeIds, $rewardInfo")
                val ad = getAd() ?: return
                emitEvent(
                    AdEvent.OnReward(
                        ad = ad,
                        reward = rewardInfo?.let {
                            Reward(
                                label = it.rewardName,
                                amount = it.rewardAmount.toIntOrNull() ?: 0
                            )
                        }
                    )
                )
                emitEvent(AdEvent.Closed(ad))
                this@MintegralRewardedImpl.rewardedAd = null
            }

            override fun onShowFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onShowFail $mBridgeIds", Throwable(message))
                emitEvent(AdEvent.ShowFailed(BidonError.Unspecified(demandId, Throwable(message))))
            }

            override fun onVideoComplete(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoComplete $mBridgeIds")
            }

            override fun onEndcardShow(mBridgeIds: MBridgeIds?) {}
        })
        handler.loadFromBid(adParams.payload)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        rewardedAd?.showFromBid() ?: run {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        rewardedAd?.clearVideoCache()
        rewardedAd = null
    }

    private fun fillAd() {
        logInfo(TAG, "Starting fill: $this")
        val ad = getAd()
        if (rewardedAd != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "MintegralRewardedImpl"