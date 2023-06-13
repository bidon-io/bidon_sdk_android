package org.bidon.sdk.ads

import org.bidon.sdk.adapter.DemandAd
import java.util.*

/**
 * Created by Bidon Team on 06/02/2023.
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
    /**
     * Source demand's ad object (instance)
     */
    val demandAdObject: Any,
) {
    override fun toString(): String {
        return "Ad(network=$networkName, adType=${demandAd.adType}, price=$ecpm, auctionId=$auctionId, round=$roundId, dsp=$dsp, currency=$currencyCode, adUnitId=$adUnitId, extras=${demandAd.getExtras()} ,demandAdObject=${demandAdObject::class.java.simpleName})"
    }
}
