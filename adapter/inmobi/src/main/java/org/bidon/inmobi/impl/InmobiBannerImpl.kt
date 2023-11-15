package org.bidon.inmobi.impl

import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal class InmobiBannerImpl :
    AdSource.Banner<InmobiBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl(),
    Mode.Network {

    private var bannerView: InMobiBanner? = null
    private var adMetaInfo: AdMetaInfo? = null
    private var bannerFormat: BannerFormat? = null
    private val clicked = AtomicBoolean(false)

    override val isAdReadyToShow: Boolean
        get() = bannerView != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            InmobiBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                price = lineItem.pricefloor,
                lineItem = lineItem
            )
        }
    }

    override fun load(adParams: InmobiBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        bannerFormat = adParams.bannerFormat
        val bannerView = InMobiBanner(adParams.activity.applicationContext, adParams.placementId).also {
            this.bannerView = it
        }
        bannerView.setBannerSize(adParams.bannerFormat.width, adParams.bannerFormat.height)
        bannerView.setEnableAutoRefresh(false)
        bannerView.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF)
        bannerView.setListener(object : BannerAdEventListener() {
            override fun onAdLoadSucceeded(inMobiBanner: InMobiBanner, adMetaInfo: AdMetaInfo) {
                this@InmobiBannerImpl.adMetaInfo = adMetaInfo
                logInfo(TAG, "onAdLoadSucceeded: $this")
                setPrice(adMetaInfo.bid)
                emitEvent(AdEvent.Fill(getAd() ?: return))
            }

            override fun onAdLoadFailed(inMobiBanner: InMobiBanner, status: InMobiAdRequestStatus) {
                logInfo(TAG, "Error while loading ad: ${status.statusCode} ${status.message}. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                this@InmobiBannerImpl.bannerView = null
            }

            override fun onAdClicked(inMobiBanner: InMobiBanner, map: MutableMap<Any, Any>?) {
                logInfo(TAG, "onAdClicked: $map, $this")
                if (!clicked.getAndSet(true)) {
                    emitEvent(AdEvent.Clicked(getAd() ?: return))
                }
            }

            override fun onAdImpression(inMobiBanner: InMobiBanner) {
                logInfo(TAG, "onAdImpression: $this")
                adMetaInfo?.let {
                    val ad = getAd() ?: return
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = it.bid / 1000.0,
                                precision = Precision.Precise,
                                currency = AdValue.USD,
                            )
                        )
                    )
                }
            }
        })
        bannerView.load()
    }

    override fun getAdView(): AdViewHolder? {
        val bannerFormat = bannerFormat ?: return null
        val bannerAd = bannerView ?: return null
        return AdViewHolder(bannerAd, bannerFormat.width, bannerFormat.height)
    }

    override fun destroy() {
        logInfo(TAG, "destroy")
        bannerView?.destroy()
        bannerView = null
    }
}

private const val TAG = "InmobiBannerImpl"