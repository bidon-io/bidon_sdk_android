package org.bidon.sdk.ads

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.models.AdUnit
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
    val dsp: String?,
    val currencyCode: String?,
    val adUnit: AdUnit
) {
    override fun toString(): String {
        return "Ad(${demandAd.adType} $networkName/$bidType $ecpm $currencyCode, auctionId=$auctionId, roundId=$roundId, dsp=$dsp, extras=${demandAd.getExtras()})"
    }
}
