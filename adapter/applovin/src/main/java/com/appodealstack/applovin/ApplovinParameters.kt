package com.appodealstack.applovin

import com.appodealstack.mads.demands.AdapterParameters

data class ApplovinParameters(
    val bannerAdUnitIds: List<String> = emptyList(),
    val interstitialAdUnitIds: List<String> = emptyList(),
    val rewardedAdUnitIds: List<String> = emptyList(),
): AdapterParameters