package com.appodealstack.ironsource.rewarded

import com.appodealstack.ironsource.RewardedInterceptor
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_RV_LOAD_NO_FILL
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import com.ironsource.mediationsdk.sdk.RewardedVideoListener
import kotlinx.coroutines.flow.MutableSharedFlow

internal fun MutableSharedFlow<RewardedInterceptor>.addRewardedListener() {
    val rewardedFlow = this
    IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
        override fun onAdAvailable(adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.AdReady(adInfo))
        }

        override fun onAdUnavailable() {
            rewardedFlow.tryEmit(
                RewardedInterceptor.AdLoadFailed(
                    IronSourceError(ERROR_RV_LOAD_NO_FILL, "")
                )
            )
        }

        override fun onAdOpened(adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.AdOpened(adInfo))
        }

        override fun onAdShowFailed(ironSourceError: IronSourceError?, adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.AdShowFailed(adInfo, ironSourceError))
        }

        override fun onAdClicked(placement: Placement?, adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.AdClicked(placement, adInfo))
        }

        override fun onAdRewarded(placement: Placement?, adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.Rewarded(placement, adInfo))
        }

        override fun onAdClosed(adInfo: AdInfo?) {
            rewardedFlow.tryEmit(RewardedInterceptor.AdClosed(adInfo))
        }
    })
    IronSource.setRewardedVideoListener(object : RewardedVideoListener {
        override fun onRewardedVideoAdOpened() {}
        override fun onRewardedVideoAdClosed() {}
        override fun onRewardedVideoAvailabilityChanged(available: Boolean) {}
        override fun onRewardedVideoAdRewarded(p0: Placement?) {}
        override fun onRewardedVideoAdShowFailed(p0: IronSourceError?) {}
        override fun onRewardedVideoAdClicked(p0: Placement?) {}

        override fun onRewardedVideoAdStarted() {
            rewardedFlow.tryEmit(RewardedInterceptor.Started)
        }

        override fun onRewardedVideoAdEnded() {
            rewardedFlow.tryEmit(RewardedInterceptor.Ended)
        }
    })
}
