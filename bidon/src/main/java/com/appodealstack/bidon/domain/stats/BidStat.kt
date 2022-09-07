package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.domain.common.DemandId

data class BidStat(
    val roundStatus: RoundStatus?,
    val demandId: DemandId,
    val ecpm: Double?,
    val roundId: String,
    val startTs: Long?,
    val finishTs: Long?,
    val adUnitId: String?,
    val auctionId: String,
)