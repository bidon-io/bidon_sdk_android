package org.bidon.unityads.impl

import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.unityads.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 12/04/2023.
 */
internal class UnityAdsBanner :
    AdSource.Banner<UnityAdsBannerAuctionParams>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {
    private var bannerAdView: BannerView? = null
    private var adUnit: AdUnit? = null

    override var isAdReadyToShow: Boolean = false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            UnityAdsBannerAuctionParams(
                activity = activity,
                adUnit = popAdUnit(demandId) ?: error(BidonError.NoAppropriateAdUnitId),
                bannerFormat = bannerFormat,
            )
        }
    }

    override fun getAdView(): AdViewHolder? {
        val bannerAdView = bannerAdView ?: return null
        return AdViewHolder(
            networkAdview = bannerAdView,
            widthDp = bannerAdView.size.width,
            heightDp = bannerAdView.size.height
        )
    }

    override fun load(adParams: UnityAdsBannerAuctionParams) {
        adUnit = adParams.adUnit
        logInfo(TAG, "Starting with $adParams")
        adParams.activity.runOnUiThread {
            val unityBannerSize = when (adParams.bannerFormat) {
                BannerFormat.LeaderBoard -> UnityBannerSize(728, 90)
                BannerFormat.Banner -> UnityBannerSize(320, 50)
                BannerFormat.Adaptive -> if (DeviceInfo.isTablet) {
                    UnityBannerSize(728, 90)
                } else {
                    UnityBannerSize(320, 50)
                }

                BannerFormat.MRec -> UnityBannerSize(300, 250)
            }
            val adView = BannerView(adParams.activity, adParams.placementId, unityBannerSize).also {
                bannerAdView = it
            }
            adView.listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView?) {
                    this@UnityAdsBanner.bannerAdView = bannerAdView
                    isAdReadyToShow = true
                    getAd()?.let {
                        emitEvent(AdEvent.Fill(it))
                    }
                }

                override fun onBannerShown(bannerAdView: BannerView?) {}

                override fun onBannerClick(bannerAdView: BannerView?) {
                    logInfo(TAG, "onAdClicked: $this")
                    getAd()?.let {
                        emitEvent(AdEvent.Clicked(it))
                    }
                }

                override fun onBannerFailedToLoad(
                    bannerAdView: BannerView?,
                    errorInfo: BannerErrorInfo?
                ) {
                    val cause = errorInfo.asBidonError()
                    logError(TAG, "Error while loading ad: $errorInfo. $this", cause)
                    isAdReadyToShow = false
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
                }

                override fun onBannerLeftApplication(bannerView: BannerView?) {
                }
            }
            adView.load()
        }
    }

    override fun destroy() {
        bannerAdView?.listener = null
        bannerAdView?.destroy()
        bannerAdView = null
    }
}

private const val TAG = "UnityAdsBanner"