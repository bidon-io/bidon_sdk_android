package org.bidon.startio.impl

import android.view.View
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.ads.banner.Mrec
import com.startapp.sdk.ads.banner.bannerstandard.BannerStandard
import com.startapp.sdk.adsbase.model.AdPreferences
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
import org.bidon.startio.StartIoDemandId

internal class StartIoBannerImpl :
    AdSource.Banner<StartIoBannerAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var banner: BannerStandard? = null
    private var isLoaded: Boolean = false

    private var loadListener = object : BannerListener {
        override fun onReceiveAd(ad: View?) {
            isLoaded = true
            logInfo(TAG, "Banner ad loaded successfully")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onFailedToReceiveAd(ad: View?) {
            isLoaded = false
            val errorMessage = "onFailedToReceiveAd: ${banner?.errorMessage}"
            logInfo(TAG, errorMessage)
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.Unspecified(
                        StartIoDemandId,
                        message = errorMessage
                    )
                )
            )
        }

        override fun onImpression(ad: View?) {
            logInfo(TAG, "Banner ad shown successfully")
        }

        override fun onClick(ad: View?) {
            logInfo(TAG, "Banner ad clicked")
            getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
        }
    }

    override val isAdReadyToShow: Boolean
        get() = banner != null && isLoaded

    override fun getAdView(): AdViewHolder? {
        return banner?.let { banner ->
            AdViewHolder(banner)
        }
    }

    override fun load(adParams: StartIoBannerAuctionParams) {
        if (adParams.payload == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            return
        }

        val adPreferences = AdPreferences().apply { adTag = adParams.tag }
        banner = if (adParams.bannerFormat == BannerFormat.MRec) {
            Mrec(adParams.activity, adPreferences, loadListener)
        } else {
            Banner(adParams.activity, adPreferences, loadListener)
        }.also { banner ->
            val (width, height) = adParams.bannerSize
            banner.loadAd(
                /* desirableWidthDp = */ width,
                /* desirableHeightDp = */ height,
                /* adm = */ adParams.payload
            )
        }
    }

    override fun destroy() {
        banner = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getBannerParam(auctionParamsScope)
    }
}

private const val TAG = "StartIoBannerImpl"
