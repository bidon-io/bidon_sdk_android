package org.bidon.bigoads.impl

import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
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
    AdSource.Banner<BigoAdsBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerAd: BannerAd? = null
    private var bannerSize: AdSize? = null

    override val isAdReadyToShow: Boolean
        get() = bannerAd?.isExpired == false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoAdsBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: BigoAdsBannerAuctionParams) {
        val bannerSize = when (adParams.bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER
            BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
            BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
            BannerFormat.Adaptive -> if (isTablet) {
                AdSize.LEADERBOARD
            } else {
                AdSize.BANNER
            }
        }.also { bannerSize = it }

        val slotId = adParams.slotId
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId, "slotId")))

        val loader = BannerAdLoader.Builder()
            .withAdLoadListener(object : AdLoadListener<BannerAd> {
                override fun onError(adError: AdError) {
                    val error = adError.asBidonError()
                    logError(TAG, "Error while loading ad: ${adError.code} ${adError.message}. $this", error)
                    emitEvent(AdEvent.LoadFailed(error))
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
            .build()

        val adRequestBuilder = BannerAdRequest.Builder()
        adRequestBuilder.withAdSizes(bannerSize)
        if (adParams.adUnit.bidType == BidType.RTB) {
            val payload = adParams.payload
                ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            adRequestBuilder.withBid(payload)
        }
        adRequestBuilder.withSlotId(slotId)
        adParams.activity.runOnUiThread {
            loader.loadAd(adRequestBuilder.build())
        }
    }

    override fun getAdView(): AdViewHolder? {
        val adView = bannerAd?.adView() ?: return null
        val bannerSize = bannerSize ?: return null
        return AdViewHolder(adView, bannerSize.width, bannerSize.height)
    }

    override fun destroy() {
        bannerSize = null
        bannerAd?.destroy()
        bannerAd = null
    }
}

private const val TAG = "BigoAdsBanner"