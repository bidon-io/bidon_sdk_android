package com.appodealstack.bidon.auctions.data.impl

import com.appodealstack.bidon.auctions.data.models.AuctionResponse
import com.appodealstack.bidon.auctions.domain.GetAuctionRequestUseCase
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest
import kotlinx.serialization.json.buildJsonObject

internal class GetAuctionRequestUseCaseImpl(
    private val dataProvider: DataProvider,
) : GetAuctionRequestUseCase {
    private val binders: List<DataBinderType> = listOf(DataBinderType.Device)

    override suspend fun request(): Result<AuctionResponse> {
        val bindData = dataProvider.provide(binders)
        val requestBody = buildJsonObject {
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }
        logInfo(Tag, "Request body: $requestBody")
        return JsonHttpRequest().invoke(
            path = AuctionRequestPath,
            body = requestBody.toString().toByteArray(),
        ).map { jsonResponse ->
            BidonJson.decodeFromJsonElement(AuctionResponse.serializer(), jsonResponse)
        }.onFailure {
            logError(Tag, "Error while loading auction data", it)
        }.onSuccess {
            logInfo(Tag, "Loaded auction data: $it")
        }
    }
}

private const val AuctionRequestPath = "auction"
private const val Tag = "AuctionRequestUseCase"
