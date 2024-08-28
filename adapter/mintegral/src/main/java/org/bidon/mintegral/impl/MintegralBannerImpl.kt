package org.bidon.mintegral.impl

import com.mbridge.msdk.out.BannerAdListener
import com.mbridge.msdk.out.BannerSize
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBridgeIds
import org.bidon.mintegral.MintegralBannerAuctionParam
import org.bidon.mintegral.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 *
 * [Mintegral Bidding](https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-in_app_header_bidding&lang=en)
 */
internal class MintegralBannerImpl :
    AdSource.Banner<MintegralBannerAuctionParam>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerView: MBBannerView? = null
    private var bannerSize: BannerSize? = null

    override var isAdReadyToShow: Boolean = false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MintegralBannerAuctionParam(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: MintegralBannerAuctionParam) {
        logInfo(TAG, "Starting with $adParams: $this")
        val placementId = adParams.placementId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")))
        val unitId = adParams.unitId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "unitId")))

        adParams.activity.runOnUiThread {
            val mbBannerView = MBBannerView(adParams.activity.applicationContext)
                .also { bannerView = it }
            bannerSize = adParams.bannerSize
            mbBannerView.init(bannerSize, placementId, unitId)
            mbBannerView.setAllowShowCloseBtn(false)
            mbBannerView.setRefreshTime(0)
            mbBannerView.setBannerAdListener(object : BannerAdListener {
                override fun onLoadFailed(mBridgeIds: MBridgeIds?, message: String?) {
                    logInfo(TAG, "onLoadFailed $mBridgeIds")
                    emitEvent(AdEvent.LoadFailed(message.asBidonError()))
                }

                override fun onLoadSuccessed(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "onLoadSuccessed $mBridgeIds")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Fill(ad))
                    isAdReadyToShow = true
                }

                override fun onLogImpression(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "onLogImpression $mBridgeIds")
                    val ad = getAd() ?: return
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
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onLeaveApp(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "onLeaveApp $mBridgeIds")
                }

                override fun showFullScreen(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "showFullScreen $mBridgeIds")
                }

                override fun closeFullScreen(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "closeFullScreen $mBridgeIds")
                }

                override fun onCloseBanner(mBridgeIds: MBridgeIds?) {
                    logInfo(TAG, "onCloseBanner $mBridgeIds")
                }
            })

            if (adParams.adUnit.bidType == BidType.CPM) {
                mbBannerView.load()
            } else {
                val payload = adParams.payload
                    ?: return@runOnUiThread emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
                mbBannerView.loadFromBid(payload)
            }
        }
    }

    override fun getAdView(): AdViewHolder? {
        logInfo(TAG, "Starting show: $this")
        val size = bannerSize ?: return null
        return if (isAdReadyToShow) {
            bannerView?.let {
                AdViewHolder(
                    networkAdview = it,
                    widthDp = size.width,
                    heightDp = size.height
                )
            }
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
            null
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        bannerView?.release()
        bannerView = null
        bannerSize = null
    }
}

private const val TAG = "MintegralBannerImpl"
