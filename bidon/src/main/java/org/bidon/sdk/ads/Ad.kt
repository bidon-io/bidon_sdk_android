package org.bidon.sdk.ads

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.stats.models.BidType
import java.util.*

/**
 * Created by Bidon Team on 06/02/2023.
 */
class Ad(
    val demandAd: DemandAd,
    val networkName: String?, // Monetization Network name
    val bidType: BidType,
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
        return "Ad($networkName, ${demandAd.adType}, $bidType, \$$ecpm, auctionId=$auctionId, round=$roundId, dsp=$dsp, currency=$currencyCode, adUnitId=$adUnitId, extras=${demandAd.getExtras()}, demandAdObject=${demandAdObject::class.java.simpleName})"
    }
}
