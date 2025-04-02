package org.bidon.vkads.impl

import com.my.target.ads.MyTargetView
import com.my.target.common.models.IAdLoadingError
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
import org.bidon.vkads.ext.asBidonError
import org.bidon.vkads.ext.toAdSize

internal class VkAdsBannerImpl :
    AdSource.Banner<VkAdsViewAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adView: MyTargetView? = null

    override val isAdReadyToShow: Boolean
        get() = adView != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VkAdsViewAuctionParams(
                activity = auctionParamsScope.activity,
                bannerFormat = bannerFormat,
                adUnit = auctionParamsScope.adUnit
            )
        }
    }

    override fun load(adParams: VkAdsViewAuctionParams) {
        val slotId = adParams.slotId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "slotId")))

        val adView = MyTargetView(adParams.activity)
            .also { adView = it }
        adView.setAdSize(adParams.bannerFormat.toAdSize())
        adView.setSlotId(slotId)
        adView.setRefreshAd(false)
        adView.customParams.setCustomParam("mediation", adParams.mediation)
        adView.listener = object : MyTargetView.MyTargetViewListener {
            override fun onLoad(adView: MyTargetView) {
                logInfo(TAG, "onLoad: $this")
                emitEvent(AdEvent.Fill(getAd() ?: return))
            }

            override fun onNoAd(error: IAdLoadingError, adView: MyTargetView) {
                logInfo(TAG, "onNoAd: ${error.code} ${error.message}. $this")
                emitEvent(AdEvent.LoadFailed(error.asBidonError(adParams.bannerFormat)))
            }

            override fun onShow(adView: MyTargetView) {
                logInfo(TAG, "onShow: $this")
                getAd()?.let { ad ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = adParams.price / 1000.0,
                                currency = AdValue.USD,
                                precision = Precision.Precise,
                            )
                        )
                    )
                }
            }

            override fun onClick(adView: MyTargetView) {
                logInfo(TAG, "onClick: $this")
                emitEvent(AdEvent.Clicked(getAd() ?: return))
            }
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            val bidId = adParams.bidId
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "bidId")))
            adView.loadFromBid(bidId)
        } else {
            adView.load()
        }
    }

    override fun getAdView(): AdViewHolder? = adView?.let { AdViewHolder(it) }

    override fun destroy() {
        adView?.destroy()
        adView = null
    }
}

private const val TAG = "VkAdsBannerImpl"
