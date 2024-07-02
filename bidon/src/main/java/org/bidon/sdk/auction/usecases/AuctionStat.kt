package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.StatsRequestBody

/**
 * Created by Aleksei Cherniaev on 09/06/2023.
 */
internal interface AuctionStat {
    fun markAuctionStarted(auctionId: String, adTypeParam: AdTypeParam)

    suspend fun addRoundResults(result: RoundResult.Results): RoundStat
    fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd): StatsRequestBody?
    fun markAuctionCanceled()
}
