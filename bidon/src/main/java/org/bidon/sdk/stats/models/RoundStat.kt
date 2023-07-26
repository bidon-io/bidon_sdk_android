package org.bidon.sdk.stats.models

import org.bidon.sdk.adapter.DemandId
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class RoundStat(
    val auctionId: String,
    val roundId: String,
    val pricefloor: Double,

    val demands: List<DemandStat.Network>,
    val bidding: DemandStat.Bidding?,

    val winnerDemandId: DemandId?,
    val winnerEcpm: Double?,
)
