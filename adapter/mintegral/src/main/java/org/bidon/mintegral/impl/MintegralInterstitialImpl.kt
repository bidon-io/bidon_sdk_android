package org.bidon.mintegral.impl

import android.app.Activity
import android.content.Context
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import org.bidon.mintegral.MintegralAuctionParam
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
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralInterstitialImpl :
    AdSource.Interstitial<MintegralAuctionParam>,
    AdLoadingType.Bidding<MintegralAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MintegralAuctionParam? = null
    private var interstitialAd: MBBidNewInterstitialHandler? = null
    private var mBridgeIds: MBridgeIds? = null

    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isBidReady == true

    override fun getToken(context: Context): String? = BidManager.getBuyerUid(context)

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralAuctionParam(
                activity = activity,
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required for Mintegral"
                },
                adUnitId = json?.getString("unit_id"),
                placementId = json?.getString("placement_id"),
            )
        }
    }

    override fun adRequest(adParams: MintegralAuctionParam) {
        logInfo(TAG, "Starting with $adParams: $this")
        this.adParams = adParams
        val handler = MBBidNewInterstitialHandler(
            adParams.activity,
            adParams.placementId,
            adParams.adUnitId
        ).also {
            interstitialAd = it
        }
        handler.setInterstitialVideoListener(object : NewInterstitialListener {

            override fun onResourceLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onResourceLoadSuccess $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                emitEvent(
                    AdEvent.Bid(
                        result = AuctionResult.Bidding(
                            adSource = this@MintegralInterstitialImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onResourceLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onResourceLoadFail $mBridgeIds", Throwable(message))
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdShow(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdShow $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralInterstitialImpl) ?: return
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
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralInterstitialImpl) ?: return
                emitEvent(AdEvent.Closed(ad))
            }

            override fun onShowFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onShowFail $mBridgeIds", Throwable(message))
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                emitEvent(AdEvent.ShowFailed(BidonError.Unspecified(demandId, Throwable(message))))
            }

            override fun onAdClicked(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdClicked $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralInterstitialImpl) ?: return
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

    override fun fill() {
        logInfo(TAG, "Starting fill: $this")
        val ad = getAd(this)
        if (mBridgeIds != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        if (isAdReadyToShow) {
            interstitialAd?.showFromBid()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        interstitialAd?.clearVideoCache()
        interstitialAd = null
    }
}

private const val TAG = "MintegralInterstitialImpl"