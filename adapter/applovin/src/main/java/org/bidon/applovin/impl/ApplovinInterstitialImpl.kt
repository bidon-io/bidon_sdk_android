package org.bidon.applovin.impl

import android.app.Activity
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdService
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinSdk
import org.bidon.applovin.ApplovinFullscreenAdAuctionParams
import org.bidon.applovin.ext.asBidonAdValue
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdLoadingType
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class ApplovinInterstitialImpl(
    private val applovinSdk: AppLovinSdk,
) : AdSource.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdLoadingType.Network<ApplovinFullscreenAdAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var applovinAd: AppLovinAd? = null
    private var lineItem: LineItem? = null

    private val listener by lazy {
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(TAG, "adDisplayed: $this")
                emitEvent(AdEvent.Shown(ad.asAd()))
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad.asAd(),
                        adValue = lineItem?.pricefloor.asBidonAdValue()
                    )
                )
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $this")
                emitEvent(AdEvent.Closed(ad.asAd()))
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $this")
                emitEvent(AdEvent.Clicked(ad.asAd()))
            }
        }
    }

    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override fun destroy() {
        logInfo(TAG, "destroy")
        applovinAd = null
    }

    override fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ApplovinFullscreenAdAuctionParams(
                lineItem = popLineItem(demandId) ?: error(BidonError.NoAppropriateAdUnitId),
                timeoutMs = timeout,
            )
        }
    }

    override fun fill(adParams: ApplovinFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        lineItem = adParams.lineItem
        val adService: AppLovinAdService = applovinSdk.adService
        val zoneId = adParams.lineItem.adUnitId
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                applovinAd = ad
                emitEvent(AdEvent.Fill(ad.asAd()))
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        logInfo(TAG, "Starting fill: $this")
        if (zoneId.isNullOrEmpty()) {
            adService.loadNextAd(AppLovinAdSize.INTERSTITIAL, requestListener)
        } else {
            adService.loadNextAdForZoneId(zoneId, requestListener)
        }
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val applovinAd = applovinAd
        if (applovinAd != null) {
            val adDialog = AppLovinInterstitialAd.create(applovinSdk, activity).apply {
                setAdDisplayListener(listener)
                setAdClickListener(listener)
            }
            adDialog.showAndRender(applovinAd)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.FullscreenAdNotReady))
        }
    }

    private fun AppLovinAd?.asAd(): Ad {
        return Ad(
            demandAd = demandAd,
            ecpm = lineItem?.pricefloor ?: 0.0,
            demandAdObject = this ?: demandAd,
            networkName = demandId.demandId,
            dsp = null,
            roundId = roundId,
            currencyCode = AdValue.USD,
            auctionId = auctionId,
            adUnitId = lineItem?.adUnitId
        )
    }
}

private const val TAG = "ApplovinInterstitial"
