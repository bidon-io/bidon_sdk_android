package org.bidon.sdk.stats.impl

import org.bidon.sdk.stats.DemandStat
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 03/03/2023.
 */

internal fun DemandStat?.asSuccessResultOrFail(): ResultBody {
    val isSucceed = this?.roundStatus == RoundStatus.Win
    return ResultBody(
        status = "SUCCESS".takeIf { isSucceed } ?: "FAIL",
        demandId = this?.demandId?.demandId.takeIf { isSucceed },
        ecpm = this?.ecpm.takeIf { isSucceed },
        adUnitId = this?.adUnitId.takeIf { isSucceed }
    )
}