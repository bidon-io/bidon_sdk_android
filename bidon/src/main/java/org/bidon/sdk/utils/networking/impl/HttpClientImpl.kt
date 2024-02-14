package org.bidon.sdk.utils.networking.impl

import androidx.annotation.WorkerThread
import kotlinx.coroutines.delay
import org.bidon.sdk.BuildConfig
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.networking.HttpClient
import org.bidon.sdk.utils.networking.Method
import org.bidon.sdk.utils.networking.NetworkSettings
import org.bidon.sdk.utils.networking.encoders.RequestDataDecoder
import org.bidon.sdk.utils.networking.encoders.RequestDataEncoder
import org.bidon.sdk.utils.networking.encoders.ext.decodeWith
import org.bidon.sdk.utils.networking.encoders.ext.encodeWith

internal val jsonZipHttpClient by lazy {
    HttpClientImpl(
        headers = buildMap {
            this["Content-Type"] = listOf("application/json; charset=UTF-8")
            this["X-Bidon-Version"] = listOf(BidonSdkVersion)
            NetworkSettings.basicAuthHeader?.let {
                this["Authorization"] = listOf("Basic $it")
            }
        },
        encoders = listOf(),
        decoders = listOf(),
    )
}

internal class HttpClientImpl(
    private val headers: Map<String, List<String>>,
    private val encoders: List<RequestDataEncoder>,
    private val decoders: List<RequestDataDecoder>
) : HttpClient {

    @WorkerThread
    override suspend fun enqueue(
        method: Method,
        url: String,
        body: ByteArray?,
    ): Result<RawResponse> {
        logInfo(TAG, "--> $method $url, request body: ${String(body ?: byteArrayOf())}")
        // getting headers from encoders
        val allHeaders = encoders
            .fold(
                initial = headers,
                operation = { headerMap, encoder ->
                    headerMap + encoder.getHeaders()
                }
            )
            .toMutableMap()

        // encoding data
        val requestBody = body?.encodeWith(encoders) ?: byteArrayOf()

        val rawRequest = RawRequest(
            method = method,
            url = url,
            headers = allHeaders,
            body = requestBody
        )
        val rawRequestClient = RawRequestClient()
        return rawRequestClient.execute(rawRequest)
            .map { rawResponse ->
                when (rawResponse) {
                    is RawResponse.Failure -> {
                        if (rawResponse.headers.containsKey(RetryAfter)) {
                            val retryDelay = rawResponse.headers[RetryAfter]?.firstOrNull()?.toLongOrNull()
                            val responseBody = rawResponse.responseBody?.let { String(it) }
                            retryDelay?.let {
                                logError(TAG, "Request failed. Retry after $retryDelay ms. $responseBody", rawResponse.httpError)
                                delay(it)
                                return enqueue(method, url, body)
                            }
                        }
                        rawResponse
                    }

                    is RawResponse.Success -> {
                        // decoding data
                        val data = rawResponse.requestBody?.decodeWith(
                            contentEncoding = rawResponse.contentEncoding,
                            decoders = decoders
                        )
                        // success
                        RawResponse.Success(
                            code = rawResponse.code,
                            requestBody = data,
                            contentEncoding = null,
                            headers = emptyMap()
                        )
                    }
                }
            }
    }
}

private val BidonSdkVersion by lazy { BuildConfig.ADAPTER_VERSION }
private const val RetryAfter = "Retry-After"
private const val TAG = "HttpClient"
