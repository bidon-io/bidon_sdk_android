package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.stats.models.Loss
import org.bidon.sdk.stats.usecases.SendLossRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.serializer.serialize

internal class SendLossRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : SendLossRequestUseCase {

    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.Session,
        DataBinderType.App,
        DataBinderType.User,
        DataBinderType.Geo,
        DataBinderType.Token,
        DataBinderType.Segment,
    )

    override suspend fun invoke(
        winnerDemandId: String,
        winnerEcpm: Double,
        demandAd: DemandAd,
        bodyKey: String,
        body: ImpressionRequestBody
    ): Result<BaseResponse> =
        withContext(SdkDispatchers.IO) {
            val urlPath = "loss/${demandAd.adType.code}"
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "external_winner",
                data = Loss(
                    demandId = winnerDemandId,
                    ecpm = winnerEcpm
                ),
                extras = BidonSdk.getExtras() + demandAd.getExtras()
            )
            requestBody.put(bodyKey, body.serialize())
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
                logInfo(Tag, "Loss notification $urlPath was sent successfully")
            }
        }
}

private const val Tag = "SendLossRequest"