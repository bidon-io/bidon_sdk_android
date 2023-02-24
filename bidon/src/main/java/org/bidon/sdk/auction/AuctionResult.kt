package org.bidon.sdk.auction

import org.bidon.sdk.adapter.AdSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class AuctionResult(
    val ecpm: Double,
    val adSource: AdSource<*>,
)
