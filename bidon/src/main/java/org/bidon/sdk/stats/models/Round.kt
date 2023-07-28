package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class Round(
    @field:JsonName("id")
    val id: String,
    @field:JsonName("pricefloor")
    val pricefloor: Double?,
    @field:JsonName("winner_id")
    val winnerDemandId: String?,
    @field:JsonName("winner_ecpm")
    val winnerEcpm: Double?,
    @field:JsonName("demands")
    val demands: List<DemandStat.Network>,
    @field:JsonName("bidding")
    val bidding: DemandStat.Bidding?,
) : Serializable
