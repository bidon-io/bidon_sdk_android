package org.bidon.mintegral.impl

import android.app.Activity
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import org.bidon.mintegral.MintegralAuctionParam
import org.bidon.mintegral.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralInterstitialImpl :
    AdSource.Interstitial<MintegralAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var interstitialAd: MBBidNewInterstitialHandler? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isBidReady == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralAuctionParam(
                activity = activity,
                adUnit = adUnit
            )
        }.onFailure {
            logError(TAG, "Failed to get auction param", it)
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
        val handler = MBBidNewInterstitialHandler(
            adParams.activity.applicationContext,
            adParams.placementId,
            adParams.unitId
        ).also {
            interstitialAd = it
        }
        handler.setInterstitialVideoListener(object : NewInterstitialListener {

            override fun onResourceLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onResourceLoadSuccess $mBridgeIds")
                logInfo(TAG, "Starting fill: $this")
                val ad = getAd()
                if (mBridgeIds != null && ad != null) {
                    emitEvent(AdEvent.Fill(ad))
                } else {
                    emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                }
            }

            override fun onResourceLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logInfo(TAG, "onResourceLoadFail $mBridgeIds")
                emitEvent(
                    AdEvent.LoadFailed(
                        message?.asBidonError()
                            ?: BidonError.NoFill(demandId)
                    )
                )
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
                            currency = USD
                        )
                    )
                )
            }

            override fun onAdClose(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
                logInfo(TAG, "onAdClose $mBridgeIds, $rewardInfo")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Closed(ad))
                this@MintegralInterstitialImpl.interstitialAd = null
            }

            override fun onShowFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onShowFail $mBridgeIds", Throwable(message))
                emitEvent(AdEvent.ShowFailed(BidonError.Unspecified(demandId, Throwable(message))))
            }

            override fun onAdClicked(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdClicked $mBridgeIds")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onVideoComplete(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onVideoComplete $mBridgeIds")
            }

            override fun onLoadCampaignSuccess(mBridgeIds: MBridgeIds?) {}
            override fun onAdCloseWithNIReward(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {}
            override fun onEndcardShow(mBridgeIds: MBridgeIds?) {}
        })
        handler.loadFromBid(adParams.payload)
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        interstitialAd?.showFromBid() ?: run {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.clearVideoCache()
        interstitialAd = null
    }
}

private const val TAG = "MintegralInterstitialImpl"