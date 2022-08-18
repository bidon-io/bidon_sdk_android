package com.appodealstack.bidon.utilities.ktor

import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.errors.BidonSdkError
import com.appodealstack.bidon.core.errors.ErrorResponse
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

internal class JsonHttpRequest {
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
                response.body<JsonObject>()
            }
            HttpStatusCode.InternalServerError -> {
                val jsonResponse = response.body<JsonObject>()
                val errorResponse = BidonJson.decodeFromJsonElement(ErrorResponse.serializer(), jsonResponse)
                throw BidonSdkError.InternalServerSdkError(errorResponse.error.message)
            }
            HttpStatusCode.UnprocessableEntity -> {
                val jsonResponse = response.body<JsonObject>()
                val errorResponse = BidonJson.decodeFromJsonElement(ErrorResponse.serializer(), jsonResponse)
                throw BidonSdkError.AppKeyIsInvalid(errorResponse.error.message)
            }
            else -> {
                throw BidonSdkError.UnknownError(response.status.description)
            }
        }
    }
}