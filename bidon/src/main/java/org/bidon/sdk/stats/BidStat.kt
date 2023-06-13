package org.bidon.sdk.stats

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.stats.models.RoundStatus
/**
 * Created by Bidon Team on 06/02/2023.
 */
data class BidStat(
    val roundStatus: RoundStatus?,
    val demandId: DemandId,
    val ecpm: Double?,
    val roundId: String,
    val bidStartTs: Long?,
    val bidFinishTs: Long?,
    val fillStartTs: Long?,
    val fillFinishTs: Long?,
    val adUnitId: String?,
    val auctionId: String,
)