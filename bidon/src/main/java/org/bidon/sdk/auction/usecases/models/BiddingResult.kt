package org.bidon.sdk.auction.usecases.models

import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Aleksei Cherniaev on 26/07/2023.
 */
internal sealed interface BiddingResult {

    object Idle : BiddingResult

    data class ServerBiddingStarted(
        val serverBiddingStartTs: Long
    ) : BiddingResult

    data class NoBid(
        val serverBiddingStartTs: Long,
        val serverBiddingFinishTs: Long
    ) : BiddingResult

    class FilledAd(
        val serverBiddingStartTs: Long,
        val serverBiddingFinishTs: Long,
        val adUnits: List<AdUnit>,
        val results: List<AuctionResult>
    ) : BiddingResult

    data class TimeoutReached(
        val serverBiddingStartTs: Long,
        val serverBiddingFinishTs: Long?,
    ) : BiddingResult
}