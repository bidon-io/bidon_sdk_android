package org.bidon.mobilefuse.impl

import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseBannerAd
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
import java.util.concurrent.atomic.AtomicBoolean

internal class MobileFuseBannerImpl :
    AdSource.Banner<MobileFuseBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var fuseBannerAd: MobileFuseBannerAd? = null
    private var bannerFormat: BannerFormat? = null

    /**
     * This flag is used to prevent [AdError]-callback from being exposed twice.
     */
    private var isLoaded = AtomicBoolean(false)

    override val isAdReadyToShow: Boolean get() = fuseBannerAd?.isLoaded == true

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getBannerParam(auctionParamsScope)
    }

    override fun load(adParams: MobileFuseBannerAuctionParams) {
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
            adParams.signalData ?: run {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(demandId = demandId, message = "signalData")
                    )
                )
                return
            }
        }
        this.bannerFormat = adParams.bannerFormat
        // placementId should be configured in the mediation platform UI and passed back to this method:
        val adSize = when (adParams.bannerFormat) {
            BannerFormat.Banner -> MobileFuseBannerAd.AdSize.BANNER_320x50
            BannerFormat.LeaderBoard -> MobileFuseBannerAd.AdSize.BANNER_728x90
            BannerFormat.MRec -> MobileFuseBannerAd.AdSize.BANNER_300x250
            BannerFormat.Adaptive -> if (isTablet) {
                MobileFuseBannerAd.AdSize.BANNER_728x90
            } else {
                MobileFuseBannerAd.AdSize.BANNER_320x50
            }
        }
        val bannerAd = MobileFuseBannerAd(adParams.activity, adParams.placementId, adSize).also {
            fuseBannerAd = it
        }
        bannerAd.autorefreshEnabled = false
        bannerAd.setListener(object : MobileFuseBannerAd.Listener {
            override fun onAdLoaded() {
                if (!isLoaded.getAndSet(true)) {
                    logInfo(TAG, "onAdLoaded")
                    getAd()?.let { emitEvent(AdEvent.Fill(it)) }
                }
            }

            override fun onAdNotFilled() {
                val cause = BidonError.NoFill(demandId)
                logError(TAG, "onAdNotFilled", null)
                emitEvent(AdEvent.LoadFailed(cause))
            }

            override fun onAdRendered() {
                logInfo(TAG, "onAdRendered")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = bannerAd.winningBidInfo.let { bidInfo ->
                                AdValue(
                                    adRevenue = bidInfo?.cpmPrice?.div(1000.0) ?: 0.0,
                                    currency = bidInfo?.currency ?: AdValue.USD,
                                    precision = Precision.Precise
                                )
                            }
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked")
                getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onAdExpired() {
                logInfo(TAG, "onAdExpired")
                emitEvent(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }

            override fun onAdError(adError: AdError?) {
                logError(TAG, "onAdError $adError", Throwable(adError?.errorMessage))
                when (adError) {
                    AdError.AD_ALREADY_RENDERED -> {
                        emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }

                    AdError.AD_ALREADY_LOADED -> {
                        // do nothing
                    }
                    AdError.AD_LOAD_ERROR -> {
                        if (!isLoaded.getAndSet(true)) {
                            getAd()?.let { emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId))) }
                        }
                    }

                    else -> {
                        emitEvent(
                            AdEvent.LoadFailed(
                                BidonError.Unspecified(
                                    demandId,
                                    Throwable(adError?.errorMessage)
                                )
                            )
                        )
                    }
                }
            }

            override fun onAdExpanded() {}
            override fun onAdCollapsed() {}
        })
        bannerAd.loadAdFromBiddingToken(adParams.signalData)
    }

    override fun getAdView(): AdViewHolder? {
        logInfo(TAG, "getAdView: $this")
        val bannerFormat = bannerFormat ?: return null
        return fuseBannerAd?.let {
            AdViewHolder(
                networkAdview = it,
                widthDp = bannerFormat.width,
                heightDp = bannerFormat.height
            )
        }
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        fuseBannerAd = null
    }
}

private const val TAG = "MobileFuseBannerImpl"