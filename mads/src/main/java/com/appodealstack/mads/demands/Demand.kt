package com.appodealstack.mads.demands

import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.AdUnit

interface BannerProvider {
    fun createBanner(): AdObject.AdView.BannerAdObject
}

interface NativeProvider {
    fun createNative(): AdObject.AdView.NativeAdObject
}

interface InterstitialProvider {
    fun createInterstitial(): AdObject.Fullscreen.InterstitialAdObject
}

interface RewardedProvider {
    fun createRewarded(): AdObject.Fullscreen.RewardedAdObject
}

interface Demand {
    val demandId: DemandId
    val adTypes: Set<AdType>

    fun getAdUnit(adType: AdType): AdUnit
    suspend fun init(context: Context, configParams: Bundle)
}