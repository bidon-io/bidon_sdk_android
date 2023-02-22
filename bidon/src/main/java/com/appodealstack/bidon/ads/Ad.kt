package com.appodealstack.bidon.ads

import com.appodealstack.bidon.adapter.DemandAd
import java.util.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Ad(
    val demandAd: DemandAd,
    val networkName: String?, // Monetization Network name
    val eCPM: Double,
    val roundId: String,
    val auctionId: String,
    val dsp: String?,
    val sourceAd: Any,
    val currencyCode: String?,
    val adUnitId: String?,
) {
    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(it) }

    override fun toString(): String {
        return "Ad(network=$networkName, adType=${demandAd.adType}, price=$eCPM, auctionId=$auctionId, round=$roundId, dsp=$dsp, currency=${currency?.currencyCode}, adUnitId=$adUnitId, sourceAdObject=${sourceAd::class.java.simpleName})"
    }
}
