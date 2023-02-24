package org.bidon.sdk.stats

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.stats.models.RoundStatus
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class DemandStat(
    val roundStatus: RoundStatus,
    val demandId: DemandId,
    val bidStartTs: Long?,
    val bidFinishTs: Long?,
    val fillStartTs: Long?,
    val fillFinishTs: Long?,
    val ecpm: Double?,
    val adUnitId: String?,
)