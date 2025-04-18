package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal val MaxPriceAuctionResolver: AuctionResolver by lazy {
    PriceAuctionResolver()
}

private class PriceAuctionResolver : AuctionResolver {
    override suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult> {
        return list.sortedByDescending {
            when (it) {
                is AuctionResult.Bidding -> it.adSource.getStats().price
                is AuctionResult.Network -> it.adSource.getStats().price
                is AuctionResult.AuctionFailed -> it.adUnit.pricefloor
            }
        }
    }
}
