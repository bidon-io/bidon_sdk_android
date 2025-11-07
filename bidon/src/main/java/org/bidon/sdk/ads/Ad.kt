package org.bidon.sdk.ads

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Bidon Team on 06/02/2023.
 */
public class Ad(
    public val demandAd: DemandAd,
    public val price: Double,
    public val auctionId: String,
    public val dsp: String?,
    public val currencyCode: String?,
    public val adUnit: AdUnit
) {
    // Monetization Network name
    public val networkName: String
        get() = adUnit.demandId

    public val bidType: BidType
        get() = adUnit.bidType

    override fun toString(): String {
        return "Ad(${demandAd.adType} $networkName/$bidType $price $currencyCode, auctionId=$auctionId, dsp=$dsp, extras=${demandAd.getExtras()}, $adUnit)"
    }
}