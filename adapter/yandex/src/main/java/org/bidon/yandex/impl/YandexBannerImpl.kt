package org.bidon.yandex.impl

import com.yandex.mobile.ads.banner.AdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.yandex.YandexDemandId

/**
 * Created by Aleksei Cherniaev on 17/09/2023.
 */
internal class YandexBannerImpl :
    AdSource.Banner<YandexBannerAuctionParam>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerView: BannerAdView? = null
    private var param: YandexBannerAuctionParam? = null
    override val isAdReadyToShow: Boolean
        get() = bannerView != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            YandexBannerAuctionParam(
                activity = activity,
                lineItem = popLineItem(YandexDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat
            )
        }
    }

    override fun load(adParams: YandexBannerAuctionParam) {
        logInfo(TAG, "Starting with $adParams")
        this.param = adParams
        val bannerView = BannerAdView(adParams.activity)
        bannerView.setAdUnitId(adParams.lineItem.adUnitId)
        bannerView.setAdSize(
            when (adParams.bannerFormat) {
                BannerFormat.Banner -> AdSize.BANNER_320x50
                BannerFormat.LeaderBoard -> AdSize.BANNER_728x90
                BannerFormat.MRec -> AdSize.BANNER_300x250
                BannerFormat.Adaptive -> AdSize.flexibleSize(320, 50)
            }
        )
        bannerView.setBannerAdEventListener(
            object : BannerAdEventListener {
                override fun onAdLoaded() {
                    this@YandexBannerImpl.bannerView = bannerView
                    val ad = getAd(bannerView) ?: return
                    emitEvent(AdEvent.Fill(ad))
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    logError(TAG, "onAdFailedToLoad: ${error.code} ${error.description}. $this", BidonError.NoFill(demandId))
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    val ad = getAd(this@YandexBannerImpl) ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}

                override fun onImpression(impressionData: ImpressionData?) {
                    logInfo(TAG, "onImpression: $this")
                    val ad = getAd(this@YandexBannerImpl) ?: return
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = ad,
                            adValue = AdValue(
                                adRevenue = adParams.price / 1000.0,
                                currency = AdValue.USD,
                                precision = Precision.Estimated
                            )
                        )
                    )
                }
            }
        )
        bannerView.loadAd(AdRequest.Builder().build())
    }

    override fun getAdView(): AdViewHolder? {
        logInfo(TAG, "getAdView: $this")
        return bannerView?.let {
            AdViewHolder(
                networkAdview = it,
                widthDp = it.adSize?.width ?: param?.bannerFormat?.getWidthDp() ?: 0,
                heightDp = it.adSize?.height ?: param?.bannerFormat?.getHeightDp() ?: 0
            )
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy: $this")
        bannerView?.destroy()
        bannerView = null
        param = null
    }
}

private const val TAG = "YandexBannerImpl"