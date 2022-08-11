package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdProvider

class AuctionResult(
    val ad: Ad,
    val adProvider: AdProvider
) {
    override fun toString(): String {
        return "AuctionResult($ad)"
    }
}