package org.bidon.gam.impl

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamInitParameters
import org.bidon.gam.ext.asBidonAdValue
import org.bidon.sdk.adapter.*
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

/**
 * [Test ad units](https://developers.google.com/admob/android/test-ads)
 *
 * [OnPaidEventListener](https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener)
 */
internal class GamBannerImpl(
    configParams: GamInitParameters?,
    private val getAdRequest: GetAdRequestUseCase = GetAdRequestUseCase(configParams),
    private val obtainToken: GetTokenUseCase = GetTokenUseCase(configParams),
    private val getAdAuctionParams: GetAdAuctionParamsUseCase = GetAdAuctionParamsUseCase(),
) : AdSource.Banner<GamBannerAuctionParams>,
    Mode.Bidding,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    override var isAdReadyToShow: Boolean = false

    private var isBiddingMode: Boolean = false
    private var adView: AdManagerAdView? = null
    private var price: Double? = null
    private var adSize: AdSize? = null
    private var bannerFormat: BannerFormat? = null

    override suspend fun getToken(context: Context, adTypeParam: AdTypeParam): String? {
        isBiddingMode = true
        return obtainToken(context, demandAd.adType)
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return getAdAuctionParams(auctionParamsScope, demandAd.adType, isBiddingMode)
    }

    @SuppressLint("MissingPermission")
    override fun load(adParams: GamBannerAuctionParams) {
        logInfo(TAG, "Starting with $adParams")
        price = adParams.price
        adSize = adParams.adSize
        bannerFormat = adParams.bannerFormat
        adParams.activity.runOnUiThread {
            val adRequest = getAdRequest(adParams)
            val adView = AdManagerAdView(adParams.activity.applicationContext).also {
                adView = it
            }
            val requestListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logInfo(TAG, "onAdFailedToLoad: $loadAdError. $this")
                    emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
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
            val adUnitId = when (adParams) {
                is GamBannerAuctionParams.Bidding -> adParams.adUnitId
                is GamBannerAuctionParams.Network -> adParams.adUnitId
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

private const val TAG = "GamBanner"