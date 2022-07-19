package com.appodealstack.mads.demands

import java.util.*

class Ad(
    val demandId: DemandId,
    val demandAd: DemandAd,
    val price: Double,
    val auctionRound: AuctionRound,
    val monetizationNetwork: String?,
    val dsp: String?,
    val sourceAd: Any,
    val currency: Currency?,
) {
    override fun toString(): String {
        return "Ad(demandId=${demandId.demandId}, adType=${demandAd.adType}, price=$price, $sourceAd)"
    }

    enum class AuctionRound(val roundName: String) {
        Mediation("mediation"),
        PostBid("postbid"),
    }
}