package org.bidon.admob.impl

import android.annotation.SuppressLint
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobInitParameters
import org.bidon.admob.asBidonError
import org.bidon.admob.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdViewHolder
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * [Test ad units](https://developers.google.com/admob/android/test-ads)
 *
 * [OnPaidEventListener](https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener)
 */
internal class AdmobBannerImpl(
    configParams: AdmobInitParameters?,
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(configParams),
    private val getAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Banner<AdmobBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    override var isAdReadyToShow: Boolean = false

    private var adView: AdView? = null
    private var price: Double? = null
    private var adSize: AdSize? = null
    private var bannerFormat: BannerFormat? = null

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return getAdAuctionParams(auctionParamsScope, demandAd.adType)
    }

    @SuppressLint("MissingPermission")
    override fun load(adParams: AdmobBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        val adUnitId: String = when (adParams) {
            is AdmobBannerAuctionParams.Bidding -> adParams.adUnitId
            is AdmobBannerAuctionParams.Network -> adParams.adUnitId
        } ?: run {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "adUnitId")
                )
            )
            return
        }
        price = adParams.price
        adSize = adParams.adSize
        bannerFormat = adParams.bannerFormat
        adParams.activity.runOnUiThread {
            val adRequest = getAdRequest(adParams) ?: run {
                emitEvent(
                    AdEvent.LoadFailed(
                        BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")
                    )
                )
                return@runOnUiThread
            }
            val adView = AdView(adParams.activity.applicationContext).also {
                adView = it
            }
            val requestListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logInfo(TAG, "onAdFailedToLoad: $loadAdError. $this")
                    emitEvent(AdEvent.LoadFailed(loadAdError.asBidonError()))
                }

                override fun onAdLoaded() {
                    logInfo(TAG, "onAdLoaded: $this")
                    isAdReadyToShow = true
                    getAd()?.let { emitEvent(AdEvent.Fill(ad = it)) }
                }

                override fun onAdClicked() {
                    logInfo(TAG, "onAdClicked: $this")
                    getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
                }

                override fun onAdClosed() {
                    logInfo(TAG, "onAdClosed: $this")
                    getAd()?.let { emitEvent(AdEvent.Closed(it)) }
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

                this.onPaidEventListener = OnPaidEventListener { adValue ->
                    getAd()?.let {
                        emitEvent(AdEvent.PaidRevenue(it, adValue.asBidonAdValue()))
                    }
                }
                adView.loadAd(adRequest)
            }
        }
    }

    override fun getAdView(): AdViewHolder? = adView?.let {
        val adSize = adSize ?: return null
        AdViewHolder(
            networkAdview = it,
            widthDp = adSize.width,
            heightDp = adSize.height
        )
    }

    override fun destroy() {
        logInfo(TAG, "destroy $this")
        adView?.onPaidEventListener = null
        adView = null
    }
}

private const val TAG = "AdmobBanner"
