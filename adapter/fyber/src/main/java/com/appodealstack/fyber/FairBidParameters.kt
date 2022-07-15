package com.appodealstack.fyber

import com.appodealstack.mads.demands.AdapterParameters

data class FairBidParameters(
    val appKey: String,
    val interstitialPlacementIds: List<String> = emptyList(),
    val rewardedPlacementIds: List<String> = emptyList(),
    val bannerPlacementIds: List<String> = emptyList(),
) : AdapterParameters