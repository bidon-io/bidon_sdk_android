package org.bidon.sdk.ads

import org.bidon.sdk.adapter.DemandAd
import java.util.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Ad(
    val demandAd: DemandAd,
    val networkName: String?, // Monetization Network name
    val ecpm: Double,
    val roundId: String,
    val auctionId: String,
    val adUnitId: String?,
    val dsp: String?,
    val currencyCode: String?,
    val sourceAd: Any,
) {
    val currency: Currency?
        get() = currencyCode?.let { Currency.getInstance(it) }

    override fun toString(): String {
        return "Ad(network=$networkName, adType=${demandAd.adType}, ecpm=$ecpm, auctionId=$auctionId, round=$roundId, dsp=$dsp, currency=${currency?.currencyCode}, adUnitId=$adUnitId, sourceAdObject=${sourceAd::class.java.simpleName})"
    }
}
