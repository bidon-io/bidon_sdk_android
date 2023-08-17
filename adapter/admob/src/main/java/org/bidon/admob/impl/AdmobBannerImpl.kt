package org.bidon.admob.impl

import android.annotation.SuppressLint
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.*
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.helper.getHeightDp
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * [Test ad units](https://developers.google.com/admob/android/test-ads)
 */
internal class AdmobBannerImpl :
    AdSource.Banner<AdmobBannerAuctionParams>,
    AdLoadingType.Network<AdmobBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    override var isAdReadyToShow: Boolean = false

    private var param: AdmobBannerAuctionParams? = null
    private var adView: AdView? = null

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            val lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId)
            AdmobBannerAuctionParams(
                lineItem = lineItem,
                bannerFormat = bannerFormat,
                context = activity.applicationContext,
                containerWidth = containerWidth,
                adUnitId = requireNotNull(lineItem.adUnitId)
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun fill(adParams: AdmobBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        param = adParams
        val adUnitId = adParams.lineItem.adUnitId
        if (!adUnitId.isNullOrBlank()) {
            val adView = AdView(adParams.context).also {
                adView = it
            }
            val requestListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logError(
                        TAG,
                        "Error while loading ad: $loadAdError. $this",
                        loadAdError.asBidonError()
                    )
                    emitEvent(
                        AdEvent.LoadFailed(loadAdError.asBidonError())
                    )
                }

                override fun onAdLoaded() {
                    logInfo(TAG, "onAdLoaded: $this")
                    isAdReadyToShow = true
                    emitEvent(AdEvent.Fill(ad = adView.asAd()))
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    emitEvent(AdEvent.Clicked(adView.asAd()))
                }

                override fun onAdClosed() {
                    logInfo(TAG, "onAdClosed: $this")
                    emitEvent(AdEvent.Closed(adView.asAd()))
                }

                override fun onAdImpression() {
                    logInfo(TAG, "onAdImpression: $this")
                    // tracked impression/shown by [BannerView]
                }

                override fun onAdOpened() {}
            }
            adView.apply {
                this.setAdSize(adParams.adSize)
                this.adUnitId = adUnitId
                this.adListener = requestListener

                /**
                 * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
                 */
                this.onPaidEventListener = OnPaidEventListener { adValue ->
                    emitEvent(
                        AdEvent.PaidRevenue(
                            ad = Ad(
                                demandAd = demandAd,
                                ecpm = adParams.lineItem.pricefloor,
                                demandAdObject = adView,
                                networkName = demandId.demandId,
                                dsp = null,
                                roundId = roundId,
                                currencyCode = AdValue.USD,
                                auctionId = auctionId,
                                adUnitId = adParams.lineItem.adUnitId
                            ),
                            adValue = adValue.asBidonAdValue()
                        )
                    )
                }
            }
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
                .build()
            adView.loadAd(adRequest)
        } else {
            val error = BidonError.NoAppropriateAdUnitId
            logError(
                tag = TAG,
                message = "No appropriate AdUnitId found for price_floor=${adParams.lineItem.pricefloor}",
                error = error
            )
            emitEvent(AdEvent.LoadFailed(error))
        }
    }

    override fun getAdView(): AdViewHolder? = adView?.let {
        AdViewHolder(
            networkAdview = it,
            widthDp = param?.adSize?.width ?: param?.bannerFormat.getWidthDp(),
            heightDp = param?.adSize?.height ?: param?.bannerFormat.getHeightDp()
        )
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adView?.onPaidEventListener = null
        adView = null
        param = null
    }

    private fun AdView.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = param?.lineItem?.pricefloor ?: 0.0,
            demandAdObject = this,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = adUnitId
        )
    }
}

private const val TAG = "Admob Banner"
