package org.bidon.sdk.stats.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.networking.BaseResponse

/**
 * Created by Aleksei Cherniaev on 06/04/2023.
 */
internal interface SendWinLossRequestUseCase {
    suspend operator fun invoke(
        data: WinLossRequestData
    ): Result<BaseResponse>
}

internal sealed interface WinLossRequestData {
    val demandAd: DemandAd
    val bodyKey: String
    val body: ImpressionRequestBody

    data class Loss(
        val winnerDemandId: String,
        val winnerEcpm: Double,
        override val demandAd: DemandAd,
        override val body: ImpressionRequestBody
    ) : WinLossRequestData {
        override val bodyKey: String = "bid"
    }

    data class Win(
        override val demandAd: DemandAd,
        override val body: ImpressionRequestBody
    ) : WinLossRequestData {
        override val bodyKey: String = "bid"
    }
}