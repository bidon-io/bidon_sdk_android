package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.domain.common.DemandId

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