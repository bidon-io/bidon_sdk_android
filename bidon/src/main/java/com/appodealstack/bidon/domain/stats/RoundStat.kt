package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.domain.common.DemandId
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class RoundStat(
    val auctionId: String,
    val roundId: String,
    val priceFloor: Double,

    val demands: List<DemandStat>,
    val winnerDemandId: DemandId?,
    val winnerEcpm: Double?,
)
