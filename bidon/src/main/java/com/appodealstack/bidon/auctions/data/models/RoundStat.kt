package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.adapters.DemandId
import com.appodealstack.bidon.analytics.data.models.RoundStatus

internal data class RoundStat(
    val auctionId: String,
    val roundId: String,
    val priceFloor: Double,

    val demands: List<DemandStat>,
    val winnerDemandId: DemandId?,
    val winnerEcpm: Double?,
)

internal data class DemandStat(
    val roundStatus: RoundStatus,
    val demandId: DemandId,
    val startTs: Long?,
    val finishTs: Long?,
    val ecpm: Double?,
    val adUnitId: String?,
)

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