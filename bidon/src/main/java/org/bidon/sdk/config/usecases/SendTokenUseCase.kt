package org.bidon.sdk.config.usecases

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.config.models.TokenRequestBody
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface SendTokenUseCase {
    suspend fun invoke(
        demandAd: DemandAd,
        demandId: String,
        token: String
    ): Result<BaseResponse>
}

internal class SendTokenUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : SendTokenUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
    )

    override suspend fun invoke(
        demandAd: DemandAd,
        demandId: String,
        token: String,
    ): Result<BaseResponse> = runCatching {
        return withContext(SdkDispatchers.IO) {
            val body = TokenRequestBody(
                demandId = demandId,
                biddingToken = token
            )
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "bidding",
                data = body,
                extras = BidonSdk.getExtras() + demandAd.getExtras()
            )
            logInfo(Tag, "$requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$SendBidTokenRequestPath",
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

private const val SendBidTokenRequestPath = "openrtb"
private const val Tag = "SendTokenUseCase"