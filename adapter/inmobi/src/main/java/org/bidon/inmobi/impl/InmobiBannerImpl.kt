package org.bidon.inmobi.impl

import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener
import org.bidon.inmobi.InmobiAdapter
import org.bidon.inmobi.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.ext.height
import org.bidon.sdk.ads.banner.ext.width
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal class InmobiBannerImpl :
    AdSource.Banner<InmobiBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerView: InMobiBanner? = null

    override val isAdReadyToShow: Boolean
        get() = bannerView != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val adUnit = adUnit
            InmobiBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: InmobiBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        val bannerView = InMobiBanner(adParams.activity, adParams.placementId)
            .also { this.bannerView = it }
        bannerView.setExtras(InmobiAdapter.getExtras())
        bannerView.setBannerSize(adParams.bannerFormat.width, adParams.bannerFormat.height)
        bannerView.setEnableAutoRefresh(false)
        bannerView.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF)
        bannerView.setListener(object : BannerAdEventListener() {
            override fun onAdLoadSucceeded(inMobiBanner: InMobiBanner, adMetaInfo: AdMetaInfo) {
                logInfo(TAG, "onAdLoadSucceeded: $this")
                emitEvent(AdEvent.Fill(getAd() ?: return))
            }

            override fun onAdLoadFailed(inMobiBanner: InMobiBanner, status: InMobiAdRequestStatus) {
                logInfo(TAG, "Error while loading ad: ${status.statusCode} ${status.message}. $this")
                emitEvent(AdEvent.LoadFailed(status.asBidonError()))
                this@InmobiBannerImpl.bannerView = null
            }

            override fun onAdClicked(inMobiBanner: InMobiBanner, map: MutableMap<Any, Any>?) {
                logInfo(TAG, "onAdClicked: $map, $this")
                emitEvent(AdEvent.Clicked(getAd() ?: return))
            }

            override fun onAdImpression(inMobiBanner: InMobiBanner) {
                logInfo(TAG, "onAdImpression: $this")
                val ad = getAd() ?: return
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = adParams.price / 1000.0,
                            precision = Precision.Precise,
                            currency = AdValue.USD,
                        )
                    )
                )
            }
        })
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
            if (payload != null) {
                bannerView.load(payload.toByteArray())
            } else {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(
                            demandId = demandId,
                            message = "payload"
                        )
                    )
                )
            }
        } else {
            bannerView.load()
        }
    }

    override fun getAdView(): AdViewHolder? = bannerView?.let { AdViewHolder(it) }

    override fun destroy() {
        logInfo(TAG, "destroy")
        bannerView?.destroy()
        bannerView = null
    }
}

private const val TAG = "InmobiBannerImpl"