package com.appodealstack.bidon.auction.impl

import com.appodealstack.bidon.auction.AuctionResolver
import com.appodealstack.bidon.auction.AuctionResult
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal val MaxEcpmAuctionResolver: AuctionResolver by lazy {
    PriceAuctionResolver()
}

private class PriceAuctionResolver : AuctionResolver {
    override suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult> {
        return list.sortedByDescending {
            it.ecpm
        }
    }
}
