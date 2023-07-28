package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.AuctionResult

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal val MaxEcpmAuctionResolver: AuctionResolver by lazy {
    PriceAuctionResolver()
}

private class PriceAuctionResolver : AuctionResolver {
    override suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult> {
        return list.sortedByDescending {
            when (it) {
                is AuctionResult.Bidding -> it.adSource.getStats().ecpm
                is AuctionResult.Network -> it.adSource.getStats().ecpm
                is AuctionResult.UnknownAdapter -> Double.MIN_VALUE
            }
        }
    }
}
