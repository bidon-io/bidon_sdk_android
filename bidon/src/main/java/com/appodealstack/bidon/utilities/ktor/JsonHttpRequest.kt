package com.appodealstack.bidon.utilities.ktor

import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.errors.BaseResponse
import com.appodealstack.bidon.core.errors.BidonSdkError
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

internal class JsonHttpRequest(
    private val keyValueStorage: KeyValueStorage
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
                throw BidonSdkError.InternalServerSdkError(errorResponse.error?.message)
            }
            HttpStatusCode.UnprocessableEntity -> {
                val jsonResponse = response.body<JsonObject>()
                val errorResponse = BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
                throw BidonSdkError.AppKeyIsInvalid(errorResponse.error?.message)
            }
            else -> {
                throw BidonSdkError.UnknownError(response.status.description)
            }
        }
    }
}

private const val Tag = "JsonHttpRequest"
