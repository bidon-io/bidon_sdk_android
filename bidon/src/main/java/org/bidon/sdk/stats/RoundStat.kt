package org.bidon.sdk.stats

import org.bidon.sdk.adapter.DemandId
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class RoundStat(
    val auctionId: String,
    val roundId: String,
    val pricefloor: Double,

    val demands: List<DemandStat>,
    val winnerDemandId: DemandId?,
    val winnerEcpm: Double?,
)
