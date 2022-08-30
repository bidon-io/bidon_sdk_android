package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.adapters.AdSource

data class AuctionResult(
    val priceFloor: Double,
    val adSource: AdSource<*>,
)
