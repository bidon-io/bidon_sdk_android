package org.bidon.mintegral.impl

import android.app.Activity
import android.content.Context
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.out.MBBidRewardVideoHandler
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import com.mbridge.msdk.out.RewardVideoListener
import org.bidon.mintegral.MintegralAuctionParam
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
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

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralRewardedImpl :
    AdSource.Rewarded<MintegralAuctionParam>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MintegralAuctionParam? = null
    private var rewardedAd: MBBidRewardVideoHandler? = null
    private var mBridgeIds: MBridgeIds? = null

    override val isAdReadyToShow: Boolean
        get() = rewardedAd?.isBidReady == true

    override suspend fun getToken(context: Context): String? = BidManager.getBuyerUid(context)

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralAuctionParam(
                activity = activity,
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required for Mintegral"
                },
                unitId = json?.getString("unit_id"),
                placementId = json?.getString("placement_id"),
            )
        }
    }

    override fun load(adParams: MintegralAuctionParam) {
        logInfo(TAG, "Starting with $adParams: $this")
        this.adParams = adParams
        val handler = MBBidRewardVideoHandler(
            adParams.activity,
            adParams.placementId,
            adParams.unitId
        ).also {
            rewardedAd = it
        }
        handler.setRewardVideoListener(object : RewardVideoListener {
            override fun onVideoLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoLoadSuccess $mBridgeIds")
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                fillAd()
            }

            override fun onLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onLoadSuccess $mBridgeIds")
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                fillAd()
            }

            override fun onVideoLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onVideoLoadFail $mBridgeIds", Throwable(message))
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onVideoAdClicked(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoAdClicked $mBridgeIds")
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralRewardedImpl) ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onAdShow(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdShow $mBridgeIds")
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralRewardedImpl) ?: return
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
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralRewardedImpl) ?: return
                emitEvent(AdEvent.Closed(ad))
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
            }

            override fun onShowFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onShowFail $mBridgeIds", Throwable(message))
                this@MintegralRewardedImpl.mBridgeIds = mBridgeIds
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
        if (isAdReadyToShow) {
            rewardedAd?.showFromBid()
        } else {
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
        val ad = getAd(this)
        if (mBridgeIds != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "MintegralRewardedImpl"