package com.appodealstack.bidon.utils.networking.impl

import androidx.annotation.WorkerThread
import com.appodealstack.bidon.BuildConfig
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.networking.HttpClient
import com.appodealstack.bidon.utils.networking.Method
import com.appodealstack.bidon.utils.networking.encoders.GZIPRequestDataEncoder
import com.appodealstack.bidon.utils.networking.encoders.RequestDataDecoder
import com.appodealstack.bidon.utils.networking.encoders.RequestDataEncoder
import com.appodealstack.bidon.utils.networking.encoders.ext.decodeWith
import com.appodealstack.bidon.utils.networking.encoders.ext.encodeWith
import kotlinx.coroutines.delay

internal val jsonZipHttpClient by lazy {
    HttpClientImpl(
        headers = mapOf(
            "Content-Type" to listOf("application/json; charset=UTF-8"),
            "X-BidOn-Version" to listOf(BidOnSdkVersion),
        ),
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
        logInfo(Tag, "--> $method $url, request body: ${String(body ?: byteArrayOf())}")
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
                                logError(Tag, "Request failed. Retry after $retryDelay ms. $responseBody", rawResponse.httpError)
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

private val BidOnSdkVersion by lazy { BuildConfig.ADAPTER_VERSION }
private const val RetryAfter = "Retry-After"
private const val Tag = "HttpClient"
