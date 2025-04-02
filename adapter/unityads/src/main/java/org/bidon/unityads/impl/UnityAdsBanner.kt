package org.bidon.unityads.impl

import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.ext.height
import org.bidon.sdk.ads.banner.ext.width
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.unityads.ext.asBidonError

/**
 * Created by Aleksei Cherniaev on 12/04/2023.
 */
internal class UnityAdsBanner :
    AdSource.Banner<UnityAdsBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {
    private var bannerAdView: BannerView? = null
    private var adUnit: AdUnit? = null

    override var isAdReadyToShow: Boolean = false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            UnityAdsBannerAuctionParams(
                activity = activity,
                adUnit = adUnit,
                bannerFormat = bannerFormat,
            )
        }
    }

    override fun load(adParams: UnityAdsBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val placementId = adParams.placementId ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "placementId")
                )
            )
            return
        }
        adUnit = adParams.adUnit
        adParams.activity.runOnUiThread {
            val unityBannerSize = UnityBannerSize(adParams.bannerFormat.width, adParams.bannerFormat.height)
            val adView = BannerView(adParams.activity, placementId, unityBannerSize).also {
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
                    logInfo(TAG, "Error while loading ad: $errorInfo. $this")
                    isAdReadyToShow = false
                    emitEvent(AdEvent.LoadFailed(errorInfo.asBidonError()))
                }

                override fun onBannerLeftApplication(bannerView: BannerView?) {
                }
            }
            adView.load()
        }
    }

    override fun getAdView(): AdViewHolder? = bannerAdView?.let { AdViewHolder(it) }

    override fun destroy() {
        bannerAdView?.listener = null
        bannerAdView?.destroy()
        bannerAdView = null
    }
}

private const val TAG = "UnityAdsBanner"