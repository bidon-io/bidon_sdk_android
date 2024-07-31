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
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType

internal class ApplovinInterstitialImpl(
    private val applovinSdk: AppLovinSdk,
) : AdSource.Interstitial<ApplovinFullscreenAdAuctionParams>,
    Mode.Network,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var applovinAd: AppLovinAd? = null
    private var adUnit: AdUnit? = null

    private val listener by lazy {
        object :
            AppLovinAdVideoPlaybackListener,
            AppLovinAdDisplayListener,
            AppLovinAdClickListener {
            override fun videoPlaybackBegan(ad: AppLovinAd) {}
            override fun videoPlaybackEnded(ad: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {}

            override fun adDisplayed(ad: AppLovinAd) {
                logInfo(TAG, "adDisplayed: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Shown(it))
                    emitEvent(AdEvent.PaidRevenue(it, adUnit?.pricefloor.asBidonAdValue()))
                }
            }

            override fun adHidden(ad: AppLovinAd) {
                logInfo(TAG, "adHidden: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Closed(it))
                }
                destroy()
            }

            override fun adClicked(ad: AppLovinAd) {
                logInfo(TAG, "adClicked: $this")
                getAd()?.let {
                    emitEvent(AdEvent.Clicked(it))
                }
            }
        }
    }

    override val isAdReadyToShow: Boolean
        get() = applovinAd != null

    override fun destroy() {
        logInfo(TAG, "destroy")
        applovinAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            ApplovinFullscreenAdAuctionParams(
                adUnit = popAdUnit(demandId, BidType.CPM) ?: error(BidonError.NoAppropriateAdUnitId),
                timeoutMs = timeout,
            )
        }
    }

    override fun load(adParams: ApplovinFullscreenAdAuctionParams) {
        logInfo(TAG, "Starting with $adParams: $this")
        adUnit = adParams.adUnit
        val adService: AppLovinAdService = applovinSdk.adService
        val zoneId = adParams.zoneId
        val requestListener = object : AppLovinAdLoadListener {
            override fun adReceived(ad: AppLovinAd) {
                logInfo(TAG, "adReceived: $this")
                applovinAd = ad
                getAd()?.let {
                    emitEvent(AdEvent.Fill(it))
                }
            }

            override fun failedToReceiveAd(errorCode: Int) {
                logInfo(TAG, "failedToReceiveAd: errorCode=$errorCode. $this")
                emitEvent(AdEvent.LoadFailed(BidonError.NoFill(demandId)))
            }
        }
        logInfo(TAG, "Starting fill: $this")
        if (zoneId.isNullOrBlank()) {
            adService.loadNextAd(AppLovinAdSize.INTERSTITIAL, requestListener)
        } else {
            adService.loadNextAdForZoneId(zoneId, requestListener)
        }
    }

    override fun show(activity: Activity) {
        logInfo(TAG, "Starting show: $this")
        val applovinAd = applovinAd
        if (applovinAd != null) {
            val adDialog = AppLovinInterstitialAd.create(applovinSdk, activity.applicationContext).apply {
                setAdDisplayListener(listener)
                setAdClickListener(listener)
            }
            adDialog.showAndRender(applovinAd)
            this.applovinAd = null
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }
}

private const val TAG = "ApplovinInterstitial"
