package org.bidon.ironsource.impl

import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyInterstitialListener
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyRewardedVideoListener
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import com.ironsource.mediationsdk.logger.IronSourceError
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo

internal val ironSourceRouter: IronSourceRouter by lazy { IronSourceRouterImpl() }

internal class IronSourceRouterImpl : IronSourceRouter {

    // I'm not sure about flow parameters, but I think it's ok
    override val adEventFlow by lazy {
        MutableSharedFlow<IronSourceEvent>(
            extraBufferCapacity = 10,
            replay = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    /**
     * Interstitial
     */
    override fun onInterstitialAdReady(instanceId: String?) {
        logInfo(TAG, "onInterstitialAdReady: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdLoaded(instanceId))
    }

    override fun onInterstitialAdLoadFailed(instanceId: String?, error: IronSourceError?) {
        logInfo(TAG, "onInterstitialAdLoadFailed: $instanceId, $error")
        adEventFlow.tryEmit(IronSourceEvent.AdLoadFailed(instanceId, error.asBidonError()))
    }

    override fun onInterstitialAdOpened(instanceId: String?) {
        logInfo(TAG, "onInterstitialAdOpened: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdOpened(instanceId))
    }

    override fun onInterstitialAdShowFailed(instanceId: String?, error: IronSourceError?) {
        logInfo(TAG, "onInterstitialAdShowFailed: $instanceId, $error")
        adEventFlow.tryEmit(IronSourceEvent.AdShowFailed(instanceId, error.asBidonError()))
    }

    override fun onInterstitialAdClicked(instanceId: String?) {
        logInfo(TAG, "onInterstitialAdClicked: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdClicked(instanceId))
    }

    override fun onInterstitialAdClosed(instanceId: String?) {
        logInfo(TAG, "onInterstitialAdClosed: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdClosed(instanceId))
    }

    /**
     * Rewarded video
     */
    override fun onRewardedVideoAdLoadSuccess(instanceId: String?) {
        logInfo(TAG, "onRewardedVideoAdLoadSuccess: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdLoaded(instanceId))
    }

    override fun onRewardedVideoAdLoadFailed(instanceId: String?, error: IronSourceError?) {
        logInfo(TAG, "onRewardedVideoAdLoadFailed: $instanceId, $error")
        adEventFlow.tryEmit(IronSourceEvent.AdLoadFailed(instanceId, error.asBidonError()))
    }

    override fun onRewardedVideoAdOpened(instanceId: String?) {
        logInfo(TAG, "onRewardedVideoAdOpened: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdOpened(instanceId))
    }

    override fun onRewardedVideoAdShowFailed(instanceId: String?, error: IronSourceError?) {
        logInfo(TAG, "onRewardedVideoAdShowFailed: $instanceId, $error")
        adEventFlow.tryEmit(IronSourceEvent.AdShowFailed(instanceId, error.asBidonError()))
    }

    override fun onRewardedVideoAdClicked(instanceId: String?) {
        logInfo(TAG, "onRewardedVideoAdClicked: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdClicked(instanceId))
    }

    override fun onRewardedVideoAdRewarded(instanceId: String?) {
        logInfo(TAG, "onRewardedVideoAdRewarded: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdRewarded(instanceId))
    }

    override fun onRewardedVideoAdClosed(instanceId: String?) {
        logInfo(TAG, "onRewardedVideoAdClosed: $instanceId")
        adEventFlow.tryEmit(IronSourceEvent.AdClosed(instanceId))
    }

    override fun onImpressionSuccess(impressionData: ImpressionData?) {
        logInfo(TAG, "onImpressionSuccess: ${impressionData?.instanceId}")
        val adValue = impressionData?.asAdValue() ?: return
        adEventFlow.tryEmit(IronSourceEvent.AdRevenuePaid(impressionData.instanceId, adValue))
    }

    private fun ImpressionData.asAdValue(): AdValue = AdValue(
        adRevenue = revenue ?: 0.0,
        precision = Precision.Precise,
        currency = AdValue.USD
    )
}

internal interface IronSourceRouter :
    ISDemandOnlyInterstitialListener,
    ISDemandOnlyRewardedVideoListener,
    ImpressionDataListener {
    val adEventFlow: SharedFlow<IronSourceEvent>
}

internal sealed interface IronSourceEvent {
    val instanceId: String?

    class AdLoaded(override val instanceId: String?) : IronSourceEvent
    class AdLoadFailed(override val instanceId: String?, val error: BidonError) : IronSourceEvent
    class AdOpened(override val instanceId: String?) : IronSourceEvent
    class AdShowFailed(override val instanceId: String?, val error: BidonError) : IronSourceEvent
    class AdClicked(override val instanceId: String?) : IronSourceEvent
    class AdClosed(override val instanceId: String?) : IronSourceEvent

    // Rewarded video
    class AdRewarded(override val instanceId: String?) : IronSourceEvent

    // Impression data
    class AdRevenuePaid(override val instanceId: String?, val adValue: AdValue) : IronSourceEvent
}

private const val TAG = "IronSourceRouterImpl"