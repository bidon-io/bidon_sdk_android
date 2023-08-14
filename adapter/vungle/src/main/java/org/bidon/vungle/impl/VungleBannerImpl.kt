package org.bidon.vungle.impl

import android.content.Context
import com.vungle.warren.Banners
import com.vungle.warren.LoadAdCallback
import com.vungle.warren.PlayAdCallback
import com.vungle.warren.Vungle
import com.vungle.warren.VungleBanner
import com.vungle.warren.error.VungleException
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.vungle.VungleBannerAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleBannerImpl :
    AdSource.Banner<VungleBannerAuctionParams>,
    AdLoadingType.Bidding<VungleBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: VungleBannerAuctionParams? = null
    private var banner: VungleBanner? = null

    override fun getToken(context: Context): String? = Vungle.getAvailableBidTokens(context)

    override val isAdReadyToShow: Boolean
        get() = adParams?.let {
            Banners.canPlayAd(
                it.bannerId,
                it.payload,
                it.bannerSize
            )
        } ?: false

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleBannerAuctionParams(
                bannerFormat = bannerFormat,
                containerWidth = containerWidth,
                bannerId = requireNotNull(json?.getString("placement_id")) {
                    "Banner id is required"
                },
                price = pricefloor,
                payload = requireNotNull(json?.getString("payload")) {
                    "Payload is required"
                }
            )
        }
    }

    override fun adRequest(adParams: VungleBannerAuctionParams) {
        this.adParams = adParams
        Banners.loadBanner(
            adParams.bannerId, adParams.payload, adParams.config,
            object : LoadAdCallback {
                override fun onAdLoad(placementId: String?) {
                    logInfo(TAG, "onAdLoad =$placementId. $this")
                    emitEvent(
                        AdEvent.Bid(
                            AuctionResult.Bidding(
                                adSource = this@VungleBannerImpl,
                                roundStatus = RoundStatus.Successful
                            )
                        )
                    )
                }

                override fun onError(placementId: String?, exception: VungleException?) {
                    logError(TAG, "onError placementId=$placementId. $this", exception)
                    emitEvent(AdEvent.LoadFailed(exception.asBidonError()))
                }
            }
        )
    }

    override fun fill() {
        val adParam = adParams
        val bidonAd = getAd(this)
        if (adParam != null && bidonAd != null) {
            banner = Banners.getBanner(
                /* placementId = */ adParam.bannerId,
                /* markup = */ adParam.payload,
                /* bannerAdConfig = */ adParam.config,
                /* playAdCallback = */ object : PlayAdCallback {
                    override fun creativeId(creativeId: String?) {}
                    override fun onAdEnd(placementId: String?, completed: Boolean, isCTAClicked: Boolean) {}
                    override fun onAdEnd(placementId: String?) {
                        logInfo(TAG, "onAdEnd: $this")
                        val ad = getAd(this@VungleBannerImpl) ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }

                    override fun onAdClick(placementId: String?) {
                        logInfo(TAG, "onAdClick: $this")
                        val ad = getAd(this@VungleBannerImpl) ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdRewarded(placementId: String?) {}

                    override fun onAdLeftApplication(placementId: String?) {}

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onAdError: $this", exception)
                        emitEvent(AdEvent.ShowFailed(exception.asBidonError()))
                    }

                    override fun onAdStart(placementId: String?) {
                        logInfo(TAG, "onAdStart: $this")
                        val ad = getAd(this@VungleBannerImpl) ?: return
                        emitEvent(AdEvent.Shown(ad))
                    }

                    override fun onAdViewed(placementId: String?) {
                        logInfo(TAG, "onAdViewed: $this")
                        val ad = getAd(this@VungleBannerImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParam.price,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }
                }
            )
            emitEvent(AdEvent.Fill(bidonAd))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }

    override fun getAdView(): AdViewHolder? {
        val adParam = adParams
        val banner = banner
        if (!isAdReadyToShow || banner == null || adParam == null) {
            AdEvent.ShowFailed(BidonError.BannerAdNotReady)
            return null
        }
        return AdViewHolder(
            networkAdview = banner,
            widthDp = adParam.bannerSize.width,
            heightDp = adParam.bannerSize.height
        )
    }

    override fun destroy() {
        banner?.destroyAd()
        banner = null
        adParams = null
    }
}

private const val TAG = "VungleBannerImpl"