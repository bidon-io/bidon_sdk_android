package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.Loss
import org.bidon.sdk.stats.usecases.SendWinLossRequestUseCase
import org.bidon.sdk.stats.usecases.WinLossRequestData
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.serializer.serialize

internal class SendWinLossRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : SendWinLossRequestUseCase {

    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.Session,
        DataBinderType.App,
        DataBinderType.User,
        DataBinderType.Token,
        DataBinderType.Segment,
        DataBinderType.Reg,
        DataBinderType.Test,
    )

    override suspend fun invoke(
        data: WinLossRequestData
    ): Result<BaseResponse> =
        withContext(SdkDispatchers.IO) {
            val path = when (data) {
                is WinLossRequestData.Loss -> "loss"
                is WinLossRequestData.Win -> "win"
            }
            val urlPath = "$path/${data.demandAd.adType.code}"
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "external_winner",
                data = when (data) {
                    is WinLossRequestData.Loss -> {
                        Loss(
                            demandId = data.winnerDemandId,
                            ecpm = data.winnerEcpm
                        )
                    }

                    is WinLossRequestData.Win -> null
                },
                extras = BidonSdk.getExtras() + data.demandAd.getExtras()
            )
            requestBody.put(data.bodyKey, data.body.serialize())
            logInfo(Tag, "Request body: $requestBody")
            get<JsonHttpRequest>().invoke(
                path = urlPath,
                body = requestBody,
            ).mapCatching { jsonResponse ->
                val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
                requireNotNull(baseResponse)
            }.onFailure {
                logError(Tag, "Error while sending loss notification $urlPath", it)
            }.onSuccess {
                logInfo(Tag, "$path-notification $urlPath was sent successfully")
            }
        }
}

private const val Tag = "SendWinLossRequest"