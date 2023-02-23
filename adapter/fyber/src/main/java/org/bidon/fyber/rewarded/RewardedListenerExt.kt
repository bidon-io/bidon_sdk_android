package org.bidon.fyber.rewarded

import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Rewarded
import com.fyber.fairbid.ads.rewarded.RewardedListener
import kotlinx.coroutines.flow.MutableSharedFlow

internal fun MutableSharedFlow<RewardedInterceptor>.initRewardedListener() {
    val rewardedInterceptorFlow = this
    Rewarded.setRewardedListener(object : RewardedListener {
        override fun onShow(placementId: String, impressionData: ImpressionData) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.Shown(placementId, impressionData)
            )
        }

        override fun onClick(placementId: String) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.Clicked(placementId)
            )
        }

        override fun onHide(placementId: String) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.Hidden(placementId)
            )
        }

        override fun onShowFailure(placementId: String, impressionData: ImpressionData) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.ShowFailed(placementId)
            )
        }

        override fun onAvailable(placementId: String) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.Loaded(placementId)
            )
        }

        override fun onUnavailable(placementId: String) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.LoadFailed(placementId)
            )
        }

        override fun onCompletion(placementId: String, userRewarded: Boolean) {
            rewardedInterceptorFlow.tryEmit(
                RewardedInterceptor.Completion(placementId, userRewarded)
            )
        }

        override fun onRequestStart(placementId: String) {}
    })
}
