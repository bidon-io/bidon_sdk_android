package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.domain.common.DemandId

internal data class RoundStat(
    val auctionId: String,
    val roundId: String,
    val priceFloor: Double,

    val demands: List<DemandStat>,
    val winnerDemandId: DemandId?,
    val winnerEcpm: Double?,
)
