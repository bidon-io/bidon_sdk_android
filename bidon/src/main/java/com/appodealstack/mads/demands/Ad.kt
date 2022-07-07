package com.appodealstack.mads.demands

class Ad(
    val demandId: DemandId,
    val demandAd: DemandAd,
    val price: Double,
    val sourceAd: Any,
) {
    override fun toString(): String {
        return "Ad(demandId=${demandId.demandId}, adType=${demandAd.adType}, price=$price, $sourceAd)"
    }
}