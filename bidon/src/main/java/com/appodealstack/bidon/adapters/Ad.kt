package com.appodealstack.bidon.adapters

import java.util.*

class Ad(
    val demandId: DemandId,
    val demandAd: DemandAd,
    val price: Double,
    val roundId: String,
    val monetizationNetwork: String?,
    val dsp: String?,
    val sourceAd: Any,
    val currencyCode: String?,
) {
    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(it) }

    override fun toString(): String {
        return "Ad(demandId=${demandId.demandId}, adType=${demandAd.adType}, price=$price, auctionRound=$roundId, monetizationNetwork=$monetizationNetwork, dsp=$dsp, currency=${currency?.currencyCode}, ${sourceAd::class.java.simpleName})"
    }

    @Deprecated("")
    enum class AuctionRound(val roundName: String) {
        Mediation("mediation"),
        PostBid("postbid"),
    }
}

const val UsdCurrencyCode = "USD"