package com.appodealstack.fyber.rewarded

import com.fyber.fairbid.ads.ImpressionData

sealed interface RewardedInterceptor {
    class Shown(val placementId: String, val impressionData: ImpressionData) : RewardedInterceptor
    data class Clicked(val placementId: String) : RewardedInterceptor
    data class Hidden(val placementId: String) : RewardedInterceptor
    data class ShowFailed(val placementId: String) : RewardedInterceptor
    data class Loaded(val placementId: String) : RewardedInterceptor
    data class LoadFailed(val placementId: String) : RewardedInterceptor
    data class RequestStarted(val placementId: String) : RewardedInterceptor
    data class Completion(val placementId: String, val userRewarded: Boolean) : RewardedInterceptor
}
