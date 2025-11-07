package org.bidon.startio.impl

import android.app.Activity
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.model.AdPreferences
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.startio.StartIoDemandId

internal class StartIoRewardedImpl :
    AdSource.Rewarded<StartIoFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var startAppAd: StartAppAd? = null

    override val isAdReadyToShow: Boolean
        get() = startAppAd?.state == Ad.AdState.READY

    private var loadListener = object : AdEventListener {
        override fun onReceiveAd(ad: Ad) {
            logInfo(TAG, "onReceiveAd")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onFailedToReceiveAd(ad: Ad?) {
            val errorMessage = "onFailedToReceiveAd: ${ad?.errorMessage}"
            logInfo(TAG, errorMessage)
            emitEvent(AdEvent.LoadFailed(BidonError.Unspecified(StartIoDemandId, message = errorMessage)))
        }
    }

    private var showListener = object : AdDisplayListener {
        override fun adHidden(ad: Ad?) {
            logInfo(TAG, "adHidden: $this")
            getAd()?.let { emitEvent(AdEvent.Closed(it)) }
        }

        override fun adDisplayed(ad: Ad?) {
            logInfo(TAG, "adDisplayed")
            getAd()?.let { emitEvent(AdEvent.Shown(it)) }
        }

        override fun adClicked(ad: Ad?) {
            logInfo(TAG, "onAdClicked")
            getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
        }

        override fun adNotDisplayed(ad: Ad?) {
            val errorMessage = "adNotDisplayed: ${ad?.errorMessage}. Reason: ${ad?.notDisplayedReason?.name}"
            logInfo(TAG, errorMessage)
            emitEvent(AdEvent.ShowFailed(BidonError.Unspecified(StartIoDemandId, message = errorMessage)))
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            startAppAd?.showAd(showListener)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun load(adParams: StartIoFullscreenAuctionParams) {
        if (adParams.payload == null) {
            emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "payload")))
            return
        }
        val startAppAd = StartAppAd(adParams.context)
            .also { this.startAppAd = it }
        startAppAd.loadAd(
            /* adMode = */ StartAppAd.AdMode.REWARDED_VIDEO,
            /* adPreferences = */ AdPreferences().apply { adTag = adParams.tag },
            /* listener = */ loadListener,
            /* adm = */ adParams.payload
        )
    }

    override fun destroy() {
        startAppAd = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }
}

private const val TAG = "StartIoRewardedImpl"
