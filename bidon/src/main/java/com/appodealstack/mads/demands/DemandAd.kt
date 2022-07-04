package com.appodealstack.mads.demands

import com.appodealstack.mads.base.AdType

class DemandAd(
    val demandId: DemandId,
    val adType: AdType,
    val objRequest: Any // Demand's Ad source object – e.x. MaxInterstitialAd
)
