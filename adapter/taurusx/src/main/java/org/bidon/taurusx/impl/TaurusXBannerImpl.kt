package org.bidon.taurusx.impl

import com.taurusx.tax.api.OnTaurusXBannerListener
import com.taurusx.tax.api.TaurusXAdError
import com.taurusx.tax.api.TaurusXBannerAds
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
import org.bidon.taurusx.ext.asBidonError

internal class TaurusXBannerImpl : AdSource.Banner<TaurusXBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(), StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerAd: TaurusXBannerAds? = null
    private var isBannerLoaded: Boolean = false
    override val isAdReadyToShow: Boolean
        get() = bannerAd != null && isBannerLoaded

    override fun getAdView(): AdViewHolder? {
        return bannerAd?.let { banner ->
            AdViewHolder(banner.adView)
        }
    }

    override fun load(adParams: TaurusXBannerAuctionParams) {
        logInfo(TAG, "Starting banner load with format: ${adParams.bannerSize}")

        val adUnitId = adParams.adUnitId
        if (adUnitId == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(
                        demandId = demandId, message = "adUnitId is required"
                    )
                )
            )
            return
        }
        val bidType = adParams.adUnit.bidType
        val payload = adParams.payload
        if (bidType == BidType.RTB && payload == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "payload is required")
                )
            )
            return
        }
        val bannerAd: TaurusXBannerAds = TaurusXBannerAds(adParams.activity).also {
            bannerAd = it
        }
        bannerAd.setAdUnitId(adUnitId)
        bannerAd.adSize = adParams.bannerSize
        bannerAd.setListener(object : OnTaurusXBannerListener {
            override fun onAdLoaded() {
                isBannerLoaded = true
                logInfo(TAG, "Banner ad loaded successfully")
                getAd()?.let { emitEvent(AdEvent.Fill(it)) }
            }

            override fun onAdShown() {
                logInfo(TAG, "Banner ad shown successfully")
                getAd()?.let { ad ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad, adValue = AdValue(
                                adRevenue = bannerAd.price.toDouble(),
                                currency = AdValue.USD,
                                precision = Precision.Precise
                            )
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(TAG, "Banner ad clicked")
                getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onAdClosed() {
                logInfo(TAG, "Banner ad closed")
            }

            override fun onAdFailedToLoad(error: TaurusXAdError?) {
                logInfo(TAG, "Banner ad load failed: ${error?.message}")
                isBannerLoaded = false
                emitEvent(AdEvent.LoadFailed(error.asBidonError()))
            }
        })
        if (bidType == BidType.RTB) {
            bannerAd.loadBannerFromBid(payload)
        } else {
            bannerAd.loadBanner()
        }
    }

    override fun destroy() {
        bannerAd?.destroy()
        bannerAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getBannerParam(auctionParamsScope)
    }
}

private const val TAG = "TaurusXBannerImpl"