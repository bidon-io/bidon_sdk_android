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
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl

internal class IronSourceRewardedAdImpl :
    AdSource.Rewarded<IronSourceFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var instanceId: String? = null
    private var observeCallbacksJob: Job? = null

    private val scope: CoroutineScope
        get() = CoroutineScope(Dispatchers.Main)

    override val isAdReadyToShow: Boolean
        get() = instanceId?.let { IronSource.isISDemandOnlyRewardedVideoAvailable(instanceId) } ?: false

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

        if (IronSource.isISDemandOnlyRewardedVideoAvailable(instanceId)) {
            logInfo(TAG, "onAdLoaded: $instanceId, $this")
            val ad = getAd() ?: return
            emitEvent(AdEvent.Fill(ad))
        } else {
            logInfo(TAG, "loadISDemandOnlyRewardedVideo: $instanceId, $this")
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
                            emitEvent(
                                AdEvent.PaidRevenue(
                                    ad = ad,
                                    adValue = AdValue(
                                        adRevenue = adParams.price / 1000.0,
                                        precision = Precision.Precise,
                                        currency = AdValue.USD,
                                    )
                                )
                            )
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

                        is IronSourceEvent.AdRewarded -> {
                            logInfo(TAG, "onAdRewarded: $instanceId, $this")
                            emitEvent(AdEvent.OnReward(ad = ad, reward = null))
                        }
                    }
                }
                .launchIn(scope)
            IronSource.loadISDemandOnlyRewardedVideo(adParams.activity, instanceId)
        }
    }

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            IronSource.showISDemandOnlyRewardedVideo(instanceId)
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

private const val TAG = "IronSourceRewardedAdImpl"
