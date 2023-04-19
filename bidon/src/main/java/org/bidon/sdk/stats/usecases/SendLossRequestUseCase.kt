package org.bidon.sdk.stats.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.networking.BaseResponse

/**
 * Created by Aleksei Cherniaev on 06/04/2023.
 */
internal interface SendLossRequestUseCase {
    suspend operator fun invoke(
        winnerDemandId: String,
        winnerEcpm: Double,
        demandAd: DemandAd,
        bodyKey: String,
        body: ImpressionRequestBody
    ): Result<BaseResponse>
}