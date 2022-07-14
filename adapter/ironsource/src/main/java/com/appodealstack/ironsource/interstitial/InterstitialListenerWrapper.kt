package com.appodealstack.ironsource.interstitial

import com.appodealstack.ironsource.InterstitialInterceptor
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import kotlinx.coroutines.flow.MutableSharedFlow

internal fun MutableSharedFlow<InterstitialInterceptor>.addInterstitialListener() {
    val interstitialFlow = this
    IronSource.setLevelPlayInterstitialListener(object : LevelPlayInterstitialListener {
        override fun onAdReady(adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdReady(adInfo))
        }

        override fun onAdLoadFailed(ironSourceError: IronSourceError?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdLoadFailed(ironSourceError))
        }

        override fun onAdOpened(adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdOpened(adInfo))
        }

        override fun onAdShowSucceeded(adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdShowSucceeded(adInfo))
        }

        override fun onAdShowFailed(ironSourceError: IronSourceError?, adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdShowFailed(adInfo, ironSourceError))
        }

        override fun onAdClicked(adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdClicked(adInfo))
        }

        override fun onAdClosed(adInfo: AdInfo?) {
            interstitialFlow.tryEmit(InterstitialInterceptor.AdClosed(adInfo))
        }
    })
}
