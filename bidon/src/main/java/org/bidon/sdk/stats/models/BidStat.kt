package org.bidon.sdk.stats.models

import org.bidon.sdk.adapter.DemandId
/**
 * Created by Bidon Team on 06/02/2023.
 */
data class BidStat(
    val auctionId: String?,
    val roundId: String?,
    val roundIndex: Int?,
    val demandId: DemandId,
    val roundStatus: RoundStatus?,

    val ecpm: Double,
    val fillStartTs: Long?,
    val fillFinishTs: Long?,
    val adUnitId: String?,
    val lineItemUid: String?,
    val dspSource: String?,

    val bidType: BidType?,
)