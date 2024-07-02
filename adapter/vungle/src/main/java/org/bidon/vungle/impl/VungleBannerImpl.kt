package org.bidon.vungle.impl

import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdSize
import com.vungle.ads.BaseAd
import com.vungle.ads.BaseAdListener
import com.vungle.ads.VungleError
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
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
import org.bidon.vungle.VungleBannerAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleBannerImpl :
    AdSource.Banner<VungleBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var banner: BannerAd? = null
    private var bannerSize: BannerAdSize? = null

    override val isAdReadyToShow: Boolean
        get() = banner?.getBannerView() != null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit,
            )
        }
    }

    override fun load(adParams: VungleBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        if (adParams.adUnit.bidType == BidType.RTB) {
            adParams.payload ?: run {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                    )
                )
                return
            }
        }
        this.bannerSize = adParams.bannerSize
        adParams.activity.runOnUiThread {
            val banner = BannerAd(
                context = adParams.activity,
                placementId = adParams.placementId,
                adSize = adParams.bannerSize
            ).also {
                this.banner = it
            }
            banner.adListener = object : BaseAdListener {
                override fun onAdLoaded(baseAd: BaseAd) {
                    val ad = getAd()
                    if (ad != null) {
                        emitEvent(AdEvent.Fill(ad))
                        logInfo(TAG, "onAdLoad =${baseAd.placementId}. $this")
                    } else {
                        emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }
                }

                override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
                    logError(TAG, "onError placementId=${baseAd.placementId}. $this", null)
                    emitEvent(AdEvent.LoadFailed(adError.asBidonError()))
                }

                override fun onAdImpression(baseAd: BaseAd) {
                    logInfo(TAG, "onAdViewed: $this")
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

                override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                    logError(TAG, "onAdError: $this", adError)
                    emitEvent(AdEvent.ShowFailed(adError.asBidonError()))
                }

                override fun onAdClicked(baseAd: BaseAd) {
                    logInfo(TAG, "onAdClick: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Clicked(ad))
                }

                override fun onAdEnd(baseAd: BaseAd) {
                    logInfo(TAG, "onAdEnd: $this")
                    val ad = getAd() ?: return
                    emitEvent(AdEvent.Closed(ad))
                }

                override fun onAdLeftApplication(baseAd: BaseAd) {}

                override fun onAdStart(baseAd: BaseAd) {}
            }
            banner.load(adParams.payload)
        }
    }

    override fun getAdView(): AdViewHolder? {
        val bannerSize = bannerSize ?: run {
            return null
        }
        val banner = banner
        if (!isAdReadyToShow || banner == null || banner.getBannerView() == null) {
            return null
        }
        return banner.getBannerView()?.let { bannerView ->
            AdViewHolder(
                networkAdview = bannerView,
                widthDp = bannerSize.width,
                heightDp = bannerSize.height
            )
        }
    }

    override fun destroy() {
        bannerSize = null
        banner?.finishAd()
        banner?.adListener = null
        banner = null
    }
}

private const val TAG = "VungleBannerImpl"