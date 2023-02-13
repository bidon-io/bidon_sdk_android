package com.appodealstack.bidon.ads

import java.util.*
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Ad(
    val demandId: DemandId,
    val demandAd: DemandAd,
    val price: Double,
    val roundId: String,
    val monetizationNetwork: String?,
    val dsp: String?,
    val sourceAd: Any,
    val currencyCode: String?,
    val auctionId: String,
) {
    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(it) }

    override fun toString(): String {
        return "Ad(demandId=${demandId.demandId}, adType=${demandAd.adType}, price=$price, auctionId=$auctionId, auctionRound=$roundId, monetizationNetwork=$monetizationNetwork, dsp=$dsp, currency=${currency?.currencyCode}, ${sourceAd::class.java.simpleName})"
    }
}
