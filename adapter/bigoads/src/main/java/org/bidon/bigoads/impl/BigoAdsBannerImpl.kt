package org.bidon.bigoads.impl

import android.content.Context
import org.bidon.bigoads.ext.asBidonError
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.RoundStatus
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
    AdLoadingType.Bidding<BigoBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var bannerAd: BannerAd? = null
    private var bannerFormat: BannerFormat? = null
    private var adParam: BigoBannerAuctionParams? = null

    override val isAdReadyToShow: Boolean
        get() = bannerAd != null

    override fun destroy() {
        bannerAd?.destroy()
        bannerAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            BigoBannerAuctionParams(
                bannerFormat = bannerFormat,
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for BigoAds banner ad"
                },
                slotId = requireNotNull(json?.optString("slot_id")) {
                    "Slot id is required for BigoAds banner ad"
                },
                bidPrice = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for BigoAds banner ad"
                },
            )
        }
    }

    override fun getAdView(): AdViewHolder? {
        val bannerAd = bannerAd ?: return null
        val width = bannerFormat?.getWidthDp() ?: return null
        val height = bannerFormat?.getHeightDp() ?: return null
        return AdViewHolder(bannerAd.adView(), width, height)
    }

    override fun getToken(context: Context): String? = BigoAdSdk.getBidderToken()

    override fun adRequest(adParams: BigoBannerAuctionParams) {
        val builder = BannerAdRequest.Builder()
        this.bannerFormat = adParams.bannerFormat
        this.adParam = adParams
        builder
            .withBid(adParams.payload)
            .withSlotId(adParams.slotId)
            .withAdSizes(
                when (adParams.bannerFormat) {
                    BannerFormat.Banner -> AdSize.BANNER
                    BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
                    BannerFormat.Adaptive -> AdSize.BANNER
                    BannerFormat.LeaderBoard -> AdSize.BANNER
                }
            )
        val loader = BannerAdLoader.Builder().withAdLoadListener(object : AdLoadListener<BannerAd> {
            override fun onError(adError: AdError) {
                val error = adError.asBidonError()
                logError(TAG, "Error while loading ad: $adError. $this", error)
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
                        val ad = getAd(this@BigoAdsBannerImpl) ?: return
                        // tracked impression/shown by [BannerView]
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = ad,
                                adValue = AdValue(
                                    adRevenue = adParam?.bidPrice ?: 0.0,
                                    precision = Precision.Precise,
                                    currency = USD,
                                )
                            )
                        )
                    }

                    override fun onAdClicked() {
                        logInfo(TAG, "onAdClicked: $this")
                        val ad = getAd(this@BigoAdsBannerImpl) ?: return
                        emitEvent(AdEvent.Clicked(ad))
                    }

                    override fun onAdOpened() {}
                    override fun onAdClosed() {}
                })
                emitEvent(
                    AdEvent.Bid(
                        AuctionResult.Bidding(
                            adSource = this@BigoAdsBannerImpl,
                            roundStatus = RoundStatus.Successful
                        )
                    )
                )
            }
        })
        loader.build()
            .loadAd(builder.build())
    }

    override fun fill() {
        val ad = getAd(this)
        if (bannerAd != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }
}

private const val TAG = "BigoAdsBanner"