package org.bidon.sdk.stats.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.utils.networking.BaseResponse

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        statsRequestBody: StatsRequestBody?,
        demandAd: DemandAd,
    ): Result<BaseResponse>
}