package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class StatsRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : StatsRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
        DataBinderType.Reg,
        DataBinderType.Test,
    )

    override suspend operator fun invoke(
        statsRequestBody: StatsRequestBody,
        demandAd: DemandAd,
    ): Result<BaseResponse> = runCatching {
        return withContext(SdkDispatchers.IO) {
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "stats",
                data = statsRequestBody,
                extras = BidonSdk.getExtras() + demandAd.getExtras()
            )
            logInfo(Tag, "$requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$StatsRequestPath/${demandAd.adType.code}",
                body = requestBody,
            ).mapCatching { jsonResponse ->
                val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
                requireNotNull(baseResponse)
            }.onFailure {
                logError(Tag, "Error while sending stats", it)
            }.onSuccess {
                logInfo(Tag, "Stats was sent successfully")
            }
        }
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"
