package org.bidon.sdk.auction
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface AuctionResolver {
    suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult>
}
