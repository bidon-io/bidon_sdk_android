package org.bidon.sdk.stats.models

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class RoundStat(
    val auctionId: String,
    val pricefloor: Double?,
    val demands: List<StatsAdUnit>,
    val noBids: List<AdUnit>?,
    val winnerDemandId: DemandId?,
    val winnerPrice: Double?,
)
