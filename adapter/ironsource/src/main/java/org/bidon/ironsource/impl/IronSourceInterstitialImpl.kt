package org.bidon.ironsource.impl

import android.app.Activity
import com.ironsource.mediationsdk.IronSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

internal class IronSourceInterstitialImpl :
    AdSource.Interstitial<IronSourceFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var instanceId: String? = null
    private var observeCallbacksJob: Job? = null

    private val scope: CoroutineScope
        get() = CoroutineScope(Dispatchers.Main)

    override val isAdReadyToShow: Boolean
        get() = instanceId?.let { IronSource.isISDemandOnlyInterstitialReady(instanceId) } ?: false

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            IronSourceFullscreenAuctionParams(
                activity = activity,
                adUnit = adUnit
            )
        }
    }

    override fun load(adParams: IronSourceFullscreenAuctionParams) {
        val instanceId = adParams.instanceId.also { this.instanceId = it }
            ?: return emitEvent(AdEvent.LoadFailed(BidonError.IncorrectAdUnit(demandId = demandId, message = "instanceId")))

        if (IronSource.isISDemandOnlyInterstitialReady(instanceId)) {
            logInfo(TAG, "onAdLoaded: $instanceId, $this")
            val ad = getAd() ?: return
            emitEvent(AdEvent.Fill(ad))
        } else {
            logInfo(TAG, "loadISDemandOnlyInterstitial: $instanceId, $this")
            observeCallbacksJob = ironSourceRouter.adEventFlow
                .filter { adEvent -> adEvent.instanceId == instanceId }
                .onEach { adEvent ->
                    val ad = getAd() ?: return@onEach
                    when (adEvent) {
                        is IronSourceEvent.AdLoaded -> {
                            logInfo(TAG, "onAdLoaded: $instanceId, $this")
                            emitEvent(AdEvent.Fill(ad))
                        }

                        is IronSourceEvent.AdLoadFailed -> {
                            logInfo(TAG, "onAdLoadFailed: $instanceId, $this")
                            emitEvent(AdEvent.LoadFailed(adEvent.error))
                            observeCallbacksJob?.cancel()
                            observeCallbacksJob = null
                        }

                        is IronSourceEvent.AdOpened -> {
                            logInfo(TAG, "onAdOpened: $instanceId, $this")
                            emitEvent(AdEvent.Shown(ad))
                        }

                        is IronSourceEvent.AdShowFailed -> {
                            logInfo(TAG, "onAdShowFailed: $instanceId, $this")
                            emitEvent(AdEvent.ShowFailed((adEvent.error)))
                        }

                        is IronSourceEvent.AdClicked -> {
                            logInfo(TAG, "onAdClicked: $instanceId, $this")
                            emitEvent(AdEvent.Clicked(ad))
                        }

                        is IronSourceEvent.AdClosed -> {
                            logInfo(TAG, "onAdClosed: $instanceId, $this")
                            emitEvent(AdEvent.Closed(ad))
                            observeCallbacksJob?.cancel()
                            observeCallbacksJob = null
                        }

                        is IronSourceEvent.AdRewarded -> {}

                        is IronSourceEvent.AdRevenuePaid -> {
                            logInfo(TAG, "onAdRevenuePaid: $instanceId, $this")
                            emitEvent(AdEvent.PaidRevenue(ad, adEvent.adValue))
                        }
                    }
                }
                .launchIn(scope)
            IronSource.loadISDemandOnlyInterstitial(adParams.activity, instanceId)
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            IronSource.showISDemandOnlyInterstitial(instanceId)
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun destroy() {
        instanceId = null
        observeCallbacksJob?.cancel()
        observeCallbacksJob = null
    }
}

private const val TAG = "IronSourceInterstitialImpl"
