package org.bidon.sdk.utils.networking

import androidx.annotation.WorkerThread
import kotlinx.coroutines.withContext
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.models.Token
import org.bidon.sdk.databinders.token.TokenDataSource
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.impl.RawResponse
import org.bidon.sdk.utils.networking.impl.jsonZipHttpClient
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class JsonHttpRequest(
    private val tokenDataSource: TokenDataSource,
) {
    @WorkerThread
    suspend operator fun invoke(
        path: String,
        body: JSONObject,
        httpClient: HttpClient = jsonZipHttpClient,
        bidOnEndpoints: BidonEndpoints = get(),
    ): Result<String> {
        val url = bidOnEndpoints.activeEndpoint + "/$path"
        return httpClient.enqueue(
            method = Method.POST,
            url = url,
            body = body.toString().toByteArray(charset = Charsets.UTF_8),
        ).mapCatching { response ->
            when (response) {
                is RawResponse.Success -> {
                    require(response.code in 200 until 300)
                    response.requestBody?.let { String(it) }.orEmpty().also {
                        logInfo(TAG, "Response: $it")
                    }
                }

                is RawResponse.Failure -> {
                    val baseResponse = JsonParsers.parseOrNull<BaseResponse>(String(response.responseBody ?: byteArrayOf()))
                    logInfo(TAG, "Request failed ${String(response.responseBody ?: byteArrayOf())}")
                    logInfo(TAG, "Request failed $baseResponse")
                    when (response.code) {
                        422 -> {
                            if ((baseResponse?.error?.message == BidonError.AppKeyIsInvalid.message)) {
                                throw BidonError.AppKeyIsInvalid
                            } else {
                                throw BidonError.NetworkError(demandId = null, message = baseResponse?.error?.message)
                            }
                        }

                        500 -> throw BidonError.InternalServerSdkError(message = baseResponse?.error?.message)
                        else -> throw BidonError.Unspecified(demandId = null, sourceError = response.httpError)
                    }
                }
            }
        }.onSuccess { jsonString ->
            withContext(SdkDispatchers.IO) {
                JSONObject(jsonString).optString("token", "").takeIf { !it.isNullOrBlank() }?.let {
                    logInfo(TAG, "New token saved: $it")
                    tokenDataSource.token = Token(token = it)
                }
            }
        }
    }
}

private const val TAG = "JsonHttpRequest"
