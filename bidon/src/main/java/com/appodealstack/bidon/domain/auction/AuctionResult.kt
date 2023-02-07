package com.appodealstack.bidon.domain.auction

import com.appodealstack.bidon.domain.adapter.AdSource
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class AuctionResult(
    val ecpm: Double,
    val adSource: AdSource<*>,
)
