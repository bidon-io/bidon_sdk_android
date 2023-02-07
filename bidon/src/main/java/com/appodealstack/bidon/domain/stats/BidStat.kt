package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.domain.common.DemandId
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
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