package org.bidon.sdk.stats.impl

import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 03/03/2023.
 */

internal fun DemandStat?.asSuccessResultOrFail(auctionStartTs: Long, auctionFinishTs: Long): ResultBody {
    val isSucceed = this?.roundStatus == RoundStatus.Win
    return ResultBody(
        status = when (this?.roundStatus) {
            RoundStatus.AuctionCancelled -> RoundStatus.AuctionCancelled.code
            RoundStatus.Win -> "SUCCESS"
            else -> "FAIL"
        },
        demandId = this?.demandId?.demandId.takeIf { isSucceed },
        ecpm = this?.ecpm.takeIf { isSucceed },
        adUnitId = (this as? DemandStat.Network)?.adUnitId.takeIf { isSucceed },
        auctionStartTs = auctionStartTs,
        auctionFinishTs = auctionFinishTs
    )
}