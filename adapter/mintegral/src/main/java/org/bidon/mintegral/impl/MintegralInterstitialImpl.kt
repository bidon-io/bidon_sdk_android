package org.bidon.mintegral.impl

import android.app.Activity
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler
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

    private var interstitialAd: MBNewInterstitialHandler? = null
    private var interstitialBidAd: MBBidNewInterstitialHandler? = null

    override val isAdReadyToShow: Boolean
        get() = (interstitialAd?.isReady == true) or (interstitialBidAd?.isBidReady == true)

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
        val placementId = adParams.placementId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")))
        val unitId = adParams.unitId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "unitId")))

        val listener = object : NewInterstitialListener {
            override fun onResourceLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onResourceLoadSuccess $mBridgeIds")
                val ad = getAd() ?: return
                emitEvent(AdEvent.Fill(ad))
            }

            override fun onResourceLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logInfo(TAG, "onResourceLoadFail $mBridgeIds")
                emitEvent(AdEvent.LoadFailed(message.asBidonError()))
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

            override fun onLoadCampaignSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onLoadCampaignSuccess $mBridgeIds")
            }

            override fun onAdCloseWithNIReward(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
                logInfo(TAG, "onAdCloseWithNIReward $mBridgeIds, $rewardInfo")
            }

            override fun onEndcardShow(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onEndcardShow $mBridgeIds")
            }
        }

        if (adParams.adUnit.bidType == BidType.CPM) {
            val handler = MBNewInterstitialHandler(adParams.activity, placementId, unitId)
            handler.setInterstitialVideoListener(listener)
            handler.load()
            interstitialAd = handler
        } else {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))

            val handler = MBBidNewInterstitialHandler(adParams.activity, placementId, unitId)
            handler.setInterstitialVideoListener(listener)
            handler.loadFromBid(payload)
            interstitialBidAd = handler
        }
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (interstitialAd?.isReady == true) {
            interstitialAd?.show()
        } else if (interstitialBidAd?.isBidReady == true) {
            interstitialBidAd?.showFromBid()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.clearVideoCache()
        interstitialAd?.setInterstitialVideoListener(null)
        interstitialAd = null

        interstitialBidAd?.clearVideoCache()
        interstitialBidAd?.setInterstitialVideoListener(null)
        interstitialBidAd = null
    }
}

private const val TAG = "MintegralInterstitialImpl"