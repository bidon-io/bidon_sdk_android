package org.bidon.mintegral.impl

import android.content.Context
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.out.BannerAdListener
import com.mbridge.msdk.out.BannerSize
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBridgeIds
import org.bidon.mintegral.MintegralBannerAuctionParam
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
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
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralBannerImpl :
    AdSource.Banner<MintegralBannerAuctionParam>,
    AdLoadingType.Bidding<MintegralBannerAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MintegralBannerAuctionParam? = null
    private var bannerView: MBBannerView? = null
    private var bannerSize: BannerSize? = null
    private var mBridgeIds: MBridgeIds? = null

    override val isAdReadyToShow: Boolean
        get() = mBridgeIds != null && bannerView != null

    override fun getToken(context: Context): String? = BidManager.getBuyerUid(context)

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralBannerAuctionParam(
                context = activity.applicationContext,
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required for Mintegral"
                },
                adUnitId = json?.getString("unit_id"),
                placementId = json?.getString("placement_id"),
                bannerFormat = bannerFormat,
            )
        }
    }

    override fun adRequest(adParams: MintegralBannerAuctionParam) {
        logInfo(TAG, "Starting with $adParams: $this")
        this.adParams = adParams
        val mbBannerView = MBBannerView(adParams.context).also {
            bannerView = it
        }
        val size = when (adParams.bannerFormat) {
            BannerFormat.Banner -> BannerSize(BannerSize.STANDARD_TYPE, 0, 0)
            BannerFormat.LeaderBoard -> BannerSize(BannerSize.LARGE_TYPE, 0, 0)
            BannerFormat.MRec -> BannerSize(BannerSize.MEDIUM_TYPE, 0, 0)
            BannerFormat.Adaptive -> BannerSize(BannerSize.SMART_TYPE, 0, 0)
        }.also {
            bannerSize = it
        }
        mbBannerView.init(size, adParams.placementId, adParams.adUnitId)
        mbBannerView.setBannerAdListener(object : BannerAdListener {
            override fun onLoadFailed(mBridgeIds: MBridgeIds?, message: String?) {
                logError(TAG, "onLoadFailed $mBridgeIds", Throwable(message))
                this@MintegralBannerImpl.mBridgeIds = mBridgeIds
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onLoadSuccessed(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onResourceLoadSuccess $mBridgeIds")
                this@MintegralBannerImpl.mBridgeIds = mBridgeIds
                emitEvent(
                    AdEvent.Bid(
                        result = AuctionResult.Bidding(
                            adSource = this@MintegralBannerImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }

            override fun onLogImpression(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onLogImpression $mBridgeIds")
                this@MintegralBannerImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralBannerImpl) ?: return
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

            override fun onClick(mBridgeIds: MBridgeIds?) {
                logInfo(TAG, "onAdClicked $mBridgeIds")
                this@MintegralBannerImpl.mBridgeIds = mBridgeIds
                val ad = getAd(this@MintegralBannerImpl) ?: return
                emitEvent(AdEvent.Clicked(ad))
            }

            override fun onLeaveApp(mBridgeIds: MBridgeIds?) {}
            override fun showFullScreen(mBridgeIds: MBridgeIds?) {}
            override fun closeFullScreen(mBridgeIds: MBridgeIds?) {}
            override fun onCloseBanner(mBridgeIds: MBridgeIds?) {}
        })
        mbBannerView.loadFromBid(adParams.payload)
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

    override fun getAdView(): AdViewHolder? {
        logInfo(TAG, "Starting show: $this")
        val size = bannerSize ?: return null
        if (isAdReadyToShow) {
            bannerView?.let {
                return AdViewHolder(
                    networkAdview = it,
                    widthDp = size.width,
                    heightDp = size.height
                )
            }
        }
        emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        return null
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        bannerView = null
        mBridgeIds = null
        bannerSize = null
    }
}

private const val TAG = "MintegralBannerImpl"
