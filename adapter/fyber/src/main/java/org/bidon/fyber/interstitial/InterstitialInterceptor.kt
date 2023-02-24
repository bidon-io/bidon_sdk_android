package org.bidon.fyber.interstitial

import com.fyber.fairbid.ads.ImpressionData

sealed interface InterstitialInterceptor {
    class Shown(val placementId: String, val impressionData: ImpressionData) : InterstitialInterceptor
    data class Clicked(val placementId: String) : InterstitialInterceptor
    data class Hidden(val placementId: String) : InterstitialInterceptor
    data class ShowFailed(val placementId: String) : InterstitialInterceptor
    data class Loaded(val placementId: String) : InterstitialInterceptor
    data class LoadFailed(val placementId: String) : InterstitialInterceptor
    data class RequestStarted(val placementId: String) : InterstitialInterceptor
}
