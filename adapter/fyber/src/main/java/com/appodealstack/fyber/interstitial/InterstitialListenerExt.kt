package com.appodealstack.fyber.interstitial

import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.interstitial.InterstitialListener
import kotlinx.coroutines.flow.MutableSharedFlow

internal fun MutableSharedFlow<InterstitialInterceptor>.initInterstitialListener() {
    val interstitialInterceptorFlow = this
    Interstitial.setInterstitialListener(object : InterstitialListener {
        override fun onShow(placementId: String, impressionData: ImpressionData) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.Shown(placementId, impressionData)
            )
        }

        override fun onClick(placementId: String) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.Clicked(placementId)
            )
        }

        override fun onHide(placementId: String) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.Hidden(placementId)
            )
        }

        override fun onShowFailure(placementId: String, impressionData: ImpressionData) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.ShowFailed(placementId)
            )
        }

        override fun onAvailable(placementId: String) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.Loaded(placementId)
            )
        }

        override fun onUnavailable(placementId: String) {
            interstitialInterceptorFlow.tryEmit(
                InterstitialInterceptor.LoadFailed(placementId)
            )
        }

        override fun onRequestStart(placementId: String) {}
    })
}
