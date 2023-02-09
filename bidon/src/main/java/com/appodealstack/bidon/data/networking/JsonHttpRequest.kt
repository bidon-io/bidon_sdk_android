package com.appodealstack.bidon.data.networking

import com.appodealstack.bidon.data.json.JsonParsers
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
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class JsonHttpRequest(
    private val keyValueStorage: KeyValueStorage,
) {
    suspend operator fun invoke(
        path: String,
        body: JSONObject,
        httpClient: HttpClient = BidonHttpClient,
        bidOnEndpoints: BidOnEndpoints = get(),
    ): Result<JSONObject> = runCatching {
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
                response.body<JSONObject>().also { jsonResponse ->
                    withContext(SdkDispatchers.IO) {
                        jsonResponse.optString("token", "").takeIf { !it.isNullOrBlank() }?.let {
                            logInternal(Tag, "New token saved: $it")
                            keyValueStorage.token = it
                        }
                    }
                }
            }
            HttpStatusCode.InternalServerError -> {
                val jsonResponse = response.body<String>()
                val errorResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
                throw BidonError.InternalServerSdkError(message = errorResponse?.error?.message)
            }
            HttpStatusCode.UnprocessableEntity -> {
                val jsonResponse = response.body<String>()
                val errorResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
                throw BidonError.AppKeyIsInvalid(message = errorResponse?.error?.message)
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
