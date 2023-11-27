package org.bidon.bigoads.impl

import android.content.Context
import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ext.height
import org.bidon.sdk.auction.ext.width
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.AdSize
import sg.bigo.ads.api.BannerAd
import sg.bigo.ads.api.BannerAdLoader
import sg.bigo.ads.api.BannerAdRequest

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal class BigoAdsBannerImpl :
    AdSource.Banner<BigoBannerAuctionParams>,
    Mode.Bidding,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerAd: BannerAd? = null
    private var bannerFormat: BannerFormat? = null

    override val isAdReadyToShow: Boolean
        get() = bannerAd != null

    override fun destroy() {
        bannerAd?.destroy()
        bannerAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            if (bannerFormat == BannerFormat.Adaptive && isTablet || bannerFormat == BannerFormat.LeaderBoard) {
                throw BidonError.AdFormatIsNotSupported(demandId.demandId, bannerFormat)
            }
            BigoBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                bidResponse = requiredBidResponse
            )
        }
    }

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam, adUnits: List<AdUnit>): String? = BigoAdSdk.getBidderToken()

    override fun load(adParams: BigoBannerAuctionParams) {
        val builder = BannerAdRequest.Builder()
        this.bannerFormat = adParams.bannerFormat
        builder.withBid(adParams.payload).withSlotId(adParams.slotId).withAdSizes(
            when (adParams.bannerFormat) {
                BannerFormat.Banner -> AdSize.BANNER
                BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
                BannerFormat.Adaptive -> AdSize.BANNER
                BannerFormat.LeaderBoard -> {
                    emitEvent(AdEvent.LoadFailed(BidonError.AdFormatIsNotSupported(demandId.demandId, adParams.bannerFormat)))
                    return
                }
            }
        )
        val loader = BannerAdLoader.Builder().withAdLoadListener(object : AdLoadListener<BannerAd> {
            override fun onError(adError: AdError) {
                val error = adError.asBidonError()
                logError(TAG, "Error while loading ad: $adError. $this", error)
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }

            override fun onAdLoaded(bannerAd: BannerAd) {
                logInfo(TAG, "onAdLoaded: $bannerAd, $this")
                this@BigoAdsBannerImpl.bannerAd = bannerAd
                bannerAd.setAdInteractionListener(object : AdInteractionListener {
                    override fun onAdError(error: AdError) {
                        val cause = error.asBidonError()
                        logError(TAG, "onAdError: $this", cause)
                        emitEvent(AdEvent.ShowFailed(cause))
                    }

                    override fun onAdImpression() {
                        logInfo(TAG, "onAdImpression: $this")
                        // tracked impression/shown by [BannerView]
                        getAd()?.let { ad ->
                            emitEvent(
                                AdEvent.PaidRevenue(
                                    ad = ad,
                                    adValue = AdValue(
                                        adRevenue = adParams.price / 1000.0,
                                        precision = Precision.Precise,
                                        currency = USD,
                                    )
                                )
                            )
                        }
                    }

                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        getAd()?.let { ad ->
                            emitEvent(AdEvent.Clicked(ad))
                        }
                    }

                    override fun onAdOpened() {}
                    override fun onAdClosed() {}
                })
                getAd()?.let { ad ->
                    emitEvent(AdEvent.Fill(ad))
                }
            }
        })
        adParams.activity.runOnUiThread {
            loader.build().loadAd(
                builder.build()
            )
        }
    }

    override fun getAdView(): AdViewHolder? {
        val bannerAd = bannerAd ?: return null
        val bannerFormat = bannerFormat ?: return null
        return AdViewHolder(bannerAd.adView(), bannerFormat.width, bannerFormat.height)
    }
}

private const val TAG = "BigoAdsBanner"