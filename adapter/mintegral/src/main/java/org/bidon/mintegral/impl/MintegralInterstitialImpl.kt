package org.bidon.mintegral.impl

import android.app.Activity
import android.content.Context
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.mintegral.MintegralAuctionParam
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.AuctionResult
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
internal class MintegralInterstitialImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String,
    private val auctionId: String,
) : AdSource.Interstitial<MintegralAuctionParam>,
    AdLoadingType.Bidding<MintegralAuctionParam>,
    StatisticsCollector by StatisticsCollectorImpl(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        demandAd = demandAd
    ) {

    private var adParams: MintegralAuctionParam? = null
    private var interstitialAd: MBBidNewInterstitialHandler? = null
    private var mBridgeIds: MBridgeIds? = null

    override val ad: Ad?
        get() = adParams?.let {
            Ad(
                ecpm = it.pricefloor,
                auctionId = auctionId,
                roundId = roundId,
                dsp = null,
                demandAdObject = this,
                currencyCode = USD,
                adUnitId = mBridgeIds?.unitId,
                demandAd = demandAd,
                networkName = demandId.demandId
            )
        }
    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean
        get() = interstitialAd?.isBidReady == true


    override fun getToken(context: Context): String? = BidManager.getBuyerUid(context)

    override fun fill() {
        logInfo(Tag, "Starting fill: $this")
        ad?.let {
            adEvent.tryEmit(AdEvent.Fill(it))
        }
    }

    override fun adRequest(adParams: MintegralAuctionParam) {
        logInfo(Tag, "Starting with $adParams: $this")
        val handler = MBBidNewInterstitialHandler(adParams.activity, "placement id", "unit id").also {
            interstitialAd = it
        }
        handler.setInterstitialVideoListener(object : NewInterstitialListener {

            override fun onResourceLoadSuccess(mBridgeIds: MBridgeIds?) {
                logInfo(Tag, "onResourceLoadSuccess $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                adEvent.tryEmit(
                    AdEvent.Bid(
                        result = AuctionResult.Bidding.Success(
                            adSource = this@MintegralInterstitialImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onResourceLoadFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(Tag, "onResourceLoadFail $mBridgeIds", Throwable(message))
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdShow(mBridgeIds: MBridgeIds?) {
                logInfo(Tag, "onAdShow $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = ad ?: return
                adEvent.tryEmit(AdEvent.Shown(ad))
                adEvent.tryEmit(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = adParams.pricefloor / 1000.0,
                            precision = Precision.Precise,
                            currency = USD
                        )
                    )
                )
            }

            override fun onAdClose(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
                logInfo(Tag, "onAdClose $mBridgeIds, $rewardInfo")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = ad ?: return
                adEvent.tryEmit(AdEvent.Closed(ad))
            }

            override fun onShowFail(mBridgeIds: MBridgeIds?, message: String?) {
                logError(Tag, "onShowFail $mBridgeIds", Throwable(message))
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                adEvent.tryEmit(AdEvent.ShowFailed(BidonError.Unspecified(demandId, Throwable(message))))
            }

            override fun onAdClicked(mBridgeIds: MBridgeIds?) {
                logInfo(Tag, "onAdClicked $mBridgeIds")
                this@MintegralInterstitialImpl.mBridgeIds = mBridgeIds
                val ad = ad ?: return
                adEvent.tryEmit(AdEvent.Clicked(ad))
            }

            override fun onVideoComplete(mBridgeIds: MBridgeIds?) {
                logInfo(Tag, "onVideoComplete $mBridgeIds")
            }
            override fun onLoadCampaignSuccess(mBridgeIds: MBridgeIds?) {}
            override fun onAdCloseWithNIReward(mBridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {}
            override fun onEndcardShow(mBridgeIds: MBridgeIds?) {}
        })
        handler.loadFromBid(adParams.payload)
    }

    override fun show(activity: Activity) {
        logInfo(Tag, "Starting show: $this")
        if (isAdReadyToShow) {
            interstitialAd?.showFromBid()
        } else {
            adEvent.tryEmit(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        interstitialAd?.clearVideoCache()
        interstitialAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralAuctionParam(
                activity = activity,
                pricefloor = pricefloor,
                payload = requireNotNull(payload) {
                    "Payload is expected non-null"
                }
            )
        }
    }
}

private const val Tag = "MintegralInterstitialImpl"