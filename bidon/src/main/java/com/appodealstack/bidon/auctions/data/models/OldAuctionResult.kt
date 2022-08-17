package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.OldAdProvider

@Deprecated("")
class OldAuctionResult(
    val ad: Ad,
    val adProvider: OldAdProvider
) {
    override fun toString(): String {
        return "AuctionResult($ad)"
    }
}

data class AuctionResult(
    val priceFloor: Double,
    val adSource: AdSource,
)