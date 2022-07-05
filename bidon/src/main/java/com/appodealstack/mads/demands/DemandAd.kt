package com.appodealstack.mads.demands

class DemandAd(
    val demandId: DemandId,
    val adType: AdType,
    val objRequest: Any // Demand's Ad source object – e.x. MaxInterstitialAd
)
