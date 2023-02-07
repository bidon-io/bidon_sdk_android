package com.appodealstack.bidon.domain.auction
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AuctionResolver {
    suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult>
}
