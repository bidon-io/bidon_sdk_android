package org.bidon.vungle.impl

import android.content.Context
import com.vungle.warren.AdConfig
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
import org.bidon.sdk.auction.AdTypeParam
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

    private var banner: VungleBanner? = null
    private var bannerSize: AdConfig.AdSize? = null
    private var payload: String? = null
    private var bannerId: String? = null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? = Vungle.getAvailableBidTokens(context)

    override val isAdReadyToShow: Boolean
        get() {
            val bannerId = bannerId ?: return false
            val payload = payload ?: return false
            val bannerSize = bannerSize ?: return false
            return Banners.canPlayAd(
                bannerId,
                payload,
                bannerSize
            )
        }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            VungleBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
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
        this.bannerSize = adParams.bannerSize
        this.payload = adParams.payload
        this.bannerId = adParams.bannerId
        adParams.activity.runOnUiThread {
            Banners.loadBanner(
                adParams.bannerId, adParams.payload, adParams.config,
                object : LoadAdCallback {
                    override fun onAdLoad(placementId: String?) {
                        logInfo(TAG, "onAdLoad =$placementId. $this")
                        adParams.activity.runOnUiThread {
                            fillAd(adParams)
                        }
                    }

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onError placementId=$placementId. $this", exception)
                        emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                    }
                }
            )
        }
    }

    private fun fillAd(adParam: VungleBannerAuctionParams) {
        val bidonAd = getAd()
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
                        val ad = getAd() ?: return
                        emitEvent(AdEvent.Closed(ad))
                    }

                    override fun onAdClick(placementId: String?) {
                        logInfo(TAG, "onAdClick: $this")
                        val ad = getAd() ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onError(placementId: String?, exception: VungleException?) {
                        logError(TAG, "onAdError: $this", exception)
                        emitEvent(AdEvent.ShowFailed(exception.asBidonError()))
                    }

                    override fun onAdViewed(placementId: String?) {
                        logInfo(TAG, "onAdViewed: $this")
                        val ad = getAd() ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParam.price / 1000.0,
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
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun getAdView(): AdViewHolder? {
        val bannerSize = bannerSize ?: run {
            AdEvent.ShowFailed(BidonError.AdNotReady)
            return null
        }
        val banner = banner
        if (!isAdReadyToShow || banner == null) {
            AdEvent.ShowFailed(BidonError.AdNotReady)
            return null
        }
        return AdViewHolder(
            networkAdview = banner.also {
                it.renderAd()
                it.setAdVisibility(true)
            },
            widthDp = bannerSize.width,
            heightDp = bannerSize.height
        )
    }

    override fun destroy() {
        banner?.destroyAd()
        banner?.setAdVisibility(false)
        banner = null
    }
}

private const val TAG = "VungleBannerImpl"