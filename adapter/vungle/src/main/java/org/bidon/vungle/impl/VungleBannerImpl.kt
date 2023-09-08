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
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.vungle.VungleBannerAuctionParams
import org.bidon.vungle.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 03/08/2023.
 */
internal class VungleBannerImpl :
    AdSource.Banner<VungleBannerAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: VungleBannerAuctionParams? = null
    private var banner: VungleBanner? = null

    override suspend fun getToken(context: Context): String? = Vungle.getAvailableBidTokens(context)

    override val isAdReadyToShow: Boolean
        get() = adParams?.let {
            Banners.canPlayAd(
                it.bannerId,
                it.payload,
                it.bannerSize
            )
        } ?: false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
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

    override fun load(adParams: VungleBannerAuctionParams) {
        this.adParams = adParams
        Banners.loadBanner(
            adParams.bannerId, adParams.payload, adParams.config,
            object : LoadAdCallback {
                override fun onAdLoad(placementId: String?) {
                    logInfo(TAG, "onAdLoad =$placementId. $this")
                    fillAd(adParams)
                }

                override fun onError(placementId: String?, exception: VungleException?) {
                    logError(TAG, "onError placementId=$placementId. $this", exception)
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }
            }
        )
    }

    private fun fillAd(adParam: VungleBannerAuctionParams) {
        val bidonAd = getAd(this)
        if (bidonAd != null) {
            this.banner = Banners.getBanner(
                /* placementId = */ adParam.bannerId,
                /* markup = */ adParam.payload,
                /* bannerAdConfig = */ adParam.config,
                /* playAdCallback = */ object : PlayAdCallback {

                    override fun onAdRewarded(placementId: String?) {}
                    override fun onAdLeftApplication(placementId: String?) {}
                    override fun creativeId(creativeId: String?) {}
                    override fun onAdStart(placementId: String?) {}

                    @Deprecated("Deprecated in Java")
                    override fun onAdEnd(placementId: String?, completed: Boolean, isCTAClicked: Boolean) {
                    }

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

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onAdError: $this", exception)
                        emitEvent(AdEvent.ShowFailed(exception.asBidonError()))
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
            ).also {
                it?.disableLifeCycleManagement(true)
            }
            emitEvent(AdEvent.Fill(bidonAd))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }

    override fun getAdView(): AdViewHolder? {
        val adParam = adParams ?: run {
            AdEvent.ShowFailed(BidonError.BannerAdNotReady)
            return null
        }
        val banner = banner
        if (!isAdReadyToShow || banner == null) {
            AdEvent.ShowFailed(BidonError.BannerAdNotReady)
            return null
        }
        return AdViewHolder(
            networkAdview = banner.also {
                it.renderAd()
                it.setAdVisibility(true)
            },
            widthDp = adParam.bannerSize.width,
            heightDp = adParam.bannerSize.height
        )
    }

    override fun destroy() {
        banner?.destroyAd()
        banner?.setAdVisibility(false)
        banner = null
        adParams = null
    }
}

private const val TAG = "VungleBannerImpl"