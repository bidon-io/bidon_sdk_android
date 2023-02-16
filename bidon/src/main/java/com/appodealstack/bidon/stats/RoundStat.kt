package com.appodealstack.bidon.stats

import com.appodealstack.bidon.adapter.DemandId
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
