package org.bidon.meta.impl

import android.content.Context
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.AdView
import com.facebook.ads.BidderTokenProvider
import org.bidon.meta.ext.asBidonError
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

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
class MetaBannerImpl :
    AdSource.Banner<MetaBannerAuctionParams>,
    AdLoadingType.Bidding<MetaBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var adParams: MetaBannerAuctionParams? = null
    private var bannerView: AdView? = null

    override val isAdReadyToShow: Boolean
        get() = bannerView != null

    override fun getToken(context: Context): String? {
        return BidderTokenProvider.getBidderToken(context)
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MetaBannerAuctionParams(
                context = activity.applicationContext,
                placementId = requireNotNull(json?.optString("placement_id")) {
                    "Placement id is required for Meta"
                },
                price = requireNotNull(json?.optDouble("price")) {
                    "Bid price is required for Meta"
                },
                payload = requireNotNull(json?.optString("payload")) {
                    "Payload is required for Meta"
                },
                bannerFormat = bannerFormat
            )
        }
    }

    override fun adRequest(adParams: MetaBannerAuctionParams) {
        this.adParams = adParams
        val banner = AdView(adParams.context, adParams.placementId, adParams.bannerSize).also {
            bannerView = it
        }
        banner.loadAd(
            banner.buildLoadAdConfig()
                .withAdListener(object : AdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        val error = adError.asBidonError()
                        logError(
                            TAG,
                            "Error while loading ad: AdError(${adError?.errorCode}: ${adError?.errorMessage}). $this",
                            error
                        )
                        emitEvent(AdEvent.LoadFailed(error))
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        logInfo(TAG, "onAdLoaded $ad: $bannerView, $this")
                        emitEvent(
                            AdEvent.Bid(
                                AuctionResult.Bidding(
                                    adSource = this@MetaBannerImpl,
                                    roundStatus = RoundStatus.Successful
                                )
                            )
                        )
                    }

                    override fun onAdClicked(ad: Ad?) {
                        logInfo(TAG, "onAdClicked: $this")
                        val bidonAd = getAd(this@MetaBannerImpl) ?: return
                        emitEvent(AdEvent.Clicked(bidonAd))
                    }

                    override fun onLoggingImpression(ad: Ad?) {
                        logInfo(TAG, "onLoggingImpression: $ad, $this")
                        val bidonAd = getAd(this@MetaBannerImpl) ?: return
                        emitEvent(
                            AdEvent.PaidRevenue(
                                ad = bidonAd,
                                adValue = AdValue(
                                    adRevenue = adParams.price,
                                    precision = Precision.Precise,
                                    currency = AdValue.USD,
                                )
                            )
                        )
                    }
                })
                .withBid(adParams.payload)
                .build()
        )
    }

    override fun fill() {
        val ad = getAd(this)
        if (bannerView != null && ad != null) {
            emitEvent(AdEvent.Fill(ad))
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.BannerAdNotReady))
        }
    }

    override fun destroy() {
        bannerView?.destroy()
        bannerView = null
        adParams = null
    }

    override fun getAdView(): AdViewHolder? {
        val adParams = adParams ?: return null
        return bannerView?.let { adView ->
            AdViewHolder(
                networkAdview = adView,
                widthDp = adParams.bannerSize.width,
                heightDp = adParams.bannerSize.height
            )
        }
    }
}

private const val TAG = "MetaBannerImpl"