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
    val currencyCode: String?,
) {
    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(it) }

    override fun toString(): String {
        return "Ad(demandId=${demandId.demandId}, adType=${demandAd.adType}, price=$price, auctionRound=$auctionRound, monetizationNetwork=$monetizationNetwork, dsp=$dsp, currency=${currency?.currencyCode}, $sourceAd)"
    }

    enum class AuctionRound(val roundName: String) {
        Mediation("mediation"),
        PostBid("postbid"),
    }
}

const val UsdCurrencyCode = "USD"