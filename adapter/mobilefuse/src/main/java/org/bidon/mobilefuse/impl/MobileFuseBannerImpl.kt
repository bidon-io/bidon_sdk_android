package org.bidon.mobilefuse.impl

import android.content.Context
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseBannerAd
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.mobilefuse.ext.GetMobileFuseTokenUseCase
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
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import java.util.concurrent.atomic.AtomicBoolean

class MobileFuseBannerImpl(private val isTestMode: Boolean) :
    AdSource.Banner<MobileFuseBannerAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var fuseBannerAd: MobileFuseBannerAd? = null
    private var bannerFormat: BannerFormat? = null

    /**
     * This flag is used to prevent [AdError]-callback from being exposed twice.
     */
    private var isLoaded = AtomicBoolean(false)

    override val adEvent = MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 1)
    override val isAdReadyToShow: Boolean get() = fuseBannerAd?.isLoaded == true

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? {
        return GetMobileFuseTokenUseCase(context, isTestMode)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getBannerParam(auctionParamsScope)
    }

    override fun load(adParams: MobileFuseBannerAuctionParams) {
        logInfo(Tag, "Starting with $adParams: $this")
        this.bannerFormat = adParams.bannerFormat
        // placementId should be configured in the mediation platform UI and passed back to this method:
        val adSize = when (adParams.bannerFormat) {
            BannerFormat.Banner -> MobileFuseBannerAd.AdSize.BANNER_320x50
            BannerFormat.LeaderBoard -> MobileFuseBannerAd.AdSize.BANNER_728x90
            BannerFormat.MRec -> MobileFuseBannerAd.AdSize.BANNER_300x250
            BannerFormat.Adaptive -> MobileFuseBannerAd.AdSize.BANNER_ADAPTIVE
        }
        val bannerAd = MobileFuseBannerAd(adParams.activity, adParams.placementId, adSize).also {
            fuseBannerAd = it
        }
        bannerAd.autorefreshEnabled = false
        bannerAd.setListener(object : MobileFuseBannerAd.Listener {
            override fun onAdLoaded() {
                if (!isLoaded.getAndSet(true)) {
                    logInfo(Tag, "onAdLoaded")
                    getAd()?.let { adEvent.tryEmit(AdEvent.Fill(it)) }
                }
            }

            override fun onAdNotFilled() {
                val cause = BidonError.NoFill(demandId)
                logError(Tag, "onAdNotFilled", cause)
                adEvent.tryEmit(AdEvent.LoadFailed(cause))
            }

            override fun onAdRendered() {
                logInfo(Tag, "onAdRendered")
                getAd()?.let {
                    adEvent.tryEmit(AdEvent.Shown(it))
                    adEvent.tryEmit(
                        AdEvent.PaidRevenue(
                            ad = it,
                            adValue = bannerAd.winningBidInfo.let { bidInfo ->
                                AdValue(
                                    adRevenue = bidInfo?.cpmPrice?.toDouble() ?: 0.0,
                                    currency = bidInfo?.currency ?: AdValue.USD,
                                    precision = Precision.Precise
                                )
                            }
                        )
                    )
                }
            }

            override fun onAdClicked() {
                logInfo(Tag, "onAdClicked")
                getAd()?.let { adEvent.tryEmit(AdEvent.Clicked(it)) }
            }

            override fun onAdExpired() {
                logInfo(Tag, "onAdExpired")
                adEvent.tryEmit(AdEvent.LoadFailed(BidonError.Expired(demandId)))
            }

            override fun onAdError(adError: AdError?) {
                logError(Tag, "onAdError $adError", Throwable(adError?.errorMessage))
                when (adError) {
                    AdError.AD_ALREADY_RENDERED -> {
                        adEvent.tryEmit(AdEvent.ShowFailed(BidonError.AdNotReady))
                    }

                    AdError.AD_ALREADY_LOADED,
                    AdError.AD_RUNTIME_ERROR -> {
                        // do nothing
                    }

                    AdError.AD_LOAD_ERROR -> {
                        if (!isLoaded.getAndSet(true)) {
                            getAd()?.let { adEvent.tryEmit(AdEvent.LoadFailed(BidonError.NoFill(demandId))) }
                        }
                    }

                    else -> {
                        // do nothing
                    }
                }
            }

            override fun onAdExpanded() {}
            override fun onAdCollapsed() {}
        })
        bannerAd.loadAdFromBiddingToken(adParams.signalData)
    }

    override fun getAdView(): AdViewHolder? {
        logInfo(Tag, "getAdView: $this")
        return fuseBannerAd?.let {
            AdViewHolder(
                networkAdview = it,
                widthDp = bannerFormat.getWidthDp(),
                heightDp = bannerFormat.getHeightDp(),
            )
        }
    }

    override fun destroy() {
        logInfo(Tag, "destroy $this")
        fuseBannerAd = null
    }
}

private const val Tag = "MobileFuseBannerImpl"