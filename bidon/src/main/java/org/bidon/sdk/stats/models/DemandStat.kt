package org.bidon.sdk.stats.models

import org.bidon.sdk.adapter.DemandId
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface DemandStat {
    val ecpm: Double?
    val roundStatus: RoundStatus
    val demandId: DemandId?

    data class Network(
        override val roundStatus: RoundStatus,
        override val demandId: DemandId,
        val bidStartTs: Long?,
        val bidFinishTs: Long?,
        val fillStartTs: Long?,
        val fillFinishTs: Long?,
        override val ecpm: Double?,
        val adUnitId: String?,
    ) : DemandStat

    data class Bidding(
        override val roundStatus: RoundStatus,
        override val demandId: DemandId?,
        val bidStartTs: Long?,
        val bidFinishTs: Long?,
        val fillStartTs: Long?,
        val fillFinishTs: Long?,
        override val ecpm: Double?,
    ) : DemandStat
}