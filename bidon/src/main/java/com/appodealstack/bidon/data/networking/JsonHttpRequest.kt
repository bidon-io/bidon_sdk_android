package com.appodealstack.bidon.data.networking

import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInternal
import com.appodealstack.bidon.view.helper.SdkDispatchers
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class JsonHttpRequest(
    private val keyValueStorage: KeyValueStorage,
) {
    suspend operator fun invoke(
        path: String,
        body: JsonObject,
        httpClient: HttpClient = BidonHttpClient,
        bidOnEndpoints: BidOnEndpoints = get(),
    ): Result<JsonObject> = runCatching {
        val response = httpClient.post {
            contentType(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTPS
                host = bidOnEndpoints.activeEndpoint.substringAfter("https://")
                path("/$path")
            }
            setBody(body)
        }
        when (response.status) {
            HttpStatusCode.OK -> {
                @Suppress("RemoveExplicitTypeArguments")
                response.body<JsonObject>().also { jsonResponse ->
                    withContext(SdkDispatchers.IO) {
                        jsonResponse.getOrDefault("token", null)?.let {
                            logInternal(Tag, "New token saved: $it")
                            keyValueStorage.token = it.toString()
                        }
                    }
                }
            }
            HttpStatusCode.InternalServerError -> {
                val jsonResponse = response.body<JsonObject>()
                val errorResponse = BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
                throw BidonError.InternalServerSdkError(message = errorResponse.error?.message)
            }
            HttpStatusCode.UnprocessableEntity -> {
                val jsonResponse = response.body<JsonObject>()
                val errorResponse = BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
                throw BidonError.AppKeyIsInvalid(message = errorResponse.error?.message)
            }
            else -> {
                logError(Tag, "Unknown error: $response")
                throw BidonError.NetworkError(
                    demandId = null,
                    message = response.status.description
                )
            }
        }
    }
}

private const val Tag = "JsonHttpRequest"
