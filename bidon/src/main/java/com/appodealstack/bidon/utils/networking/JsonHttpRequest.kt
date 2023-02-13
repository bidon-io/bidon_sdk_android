package com.appodealstack.bidon.utils.networking

import com.appodealstack.bidon.ads.BidonError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.SdkDispatchers
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utils.networking.impl.RawResponse
import com.appodealstack.bidon.utils.networking.impl.jsonZipHttpClient
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
        httpClient: HttpClient = jsonZipHttpClient,
        bidOnEndpoints: BidOnEndpoints = get(),
    ): Result<String> {
        return withContext(SdkDispatchers.IO) {
            val url = bidOnEndpoints.activeEndpoint + "/$path"
            httpClient.enqueue(
                method = Method.POST,
                url = url,
                body = body.toString().toByteArray(charset = Charsets.UTF_8),
            ).mapCatching { response ->
                when (response) {
                    is RawResponse.Success -> {
                        require(response.code in 200 until 300)
                        response.requestBody?.let { String(it) }.orEmpty().also {
                            logInfo(Tag, "Response: $it")
                        }
                    }
                    is RawResponse.Failure -> {
                        val baseResponse = JsonParsers.parseOrNull<BaseResponse>(String(response.responseBody ?: byteArrayOf()))
                        logInfo(Tag, "Request failed $baseResponse")
                        when (response.code) {
                            422 -> throw BidonError.AppKeyIsInvalid(message = baseResponse?.error?.message)
                            500 -> throw BidonError.InternalServerSdkError(message = baseResponse?.error?.message)
                            else -> throw BidonError.Unspecified(demandId = null, sourceError = response.httpError)
                        }
                    }
                }
            }.onSuccess { jsonString ->
                withContext(SdkDispatchers.IO) {
                    JSONObject(jsonString).optString("token", "").takeIf { !it.isNullOrBlank() }?.let {
                        logInfo(Tag, "New token saved: $it")
                        keyValueStorage.token = it
                    }
                }
            }
        }
    }
}

private const val Tag = "JsonHttpRequest"
