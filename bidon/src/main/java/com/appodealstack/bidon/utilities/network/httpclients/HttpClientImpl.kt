package com.appodealstack.bidon.utilities.network.httpclients

import com.appodealstack.bidon.utilities.network.HttpClient
import com.appodealstack.bidon.utilities.network.HttpError
import com.appodealstack.bidon.utilities.network.NetworkSettings.BaseAppodealUrl
import com.appodealstack.bidon.utilities.network.Networking
import com.appodealstack.bidon.utilities.network.encoders.Base64RequestDataEncoder
import com.appodealstack.bidon.utilities.network.encoders.GZIPRequestDataEncoder
import com.appodealstack.bidon.utilities.network.encoders.RequestDataDecoder
import com.appodealstack.bidon.utilities.network.encoders.RequestDataEncoder
import com.appodealstack.bidon.utilities.network.encoders.ext.decodeWith
import com.appodealstack.bidon.utilities.network.encoders.ext.encodeWith
import com.appodeal.ads.network.httpclients.verification.Verifier
import com.appodealstack.bidon.core.ext.logInternal

internal val jsonHttpClient by lazy {
    HttpClientImpl(
        headers = mapOf("Content-Type" to listOf("application/json; charset=UTF-8")),
        encoders = listOf(),
        decoders = listOf(),
    )
}

internal val zipHttpClient by lazy {
    HttpClientImpl(
        headers = mapOf("Content-Type" to listOf("text/plain; charset=UTF-8")),
        encoders = listOf(GZIPRequestDataEncoder),
        decoders = listOf(GZIPRequestDataEncoder),
    )
}

internal val zipBase64HttpClient by lazy {
    HttpClientImpl(
        headers = mapOf("Content-Type" to listOf("text/plain; charset=UTF-8")),
        encoders = listOf(GZIPRequestDataEncoder, Base64RequestDataEncoder),
        decoders = listOf(GZIPRequestDataEncoder),
    )
}

internal val protoHttpClient by lazy {
    HttpClientImpl(
        headers = mapOf("Content-Type" to listOf("application/x-protobuf")),
        encoders = listOf(GZIPRequestDataEncoder),
        decoders = listOf(GZIPRequestDataEncoder),
    )
}

internal class HttpClientImpl(
    private val headers: Map<String, List<String>>,
    private val encoders: List<RequestDataEncoder>,
    private val decoders: List<RequestDataDecoder>
) : Networking {
    private val rawRequestClient: RawRequestClient = RawRequestClient()

    override fun <Response> enqueue(
        method: HttpClient.Method,
        url: String,
        body: ByteArray?,
        parser: (response: ByteArray?) -> Response?,
        useUniqueRequestId: Boolean,
    ): Result<Response?> {
        logInternal(TAG, "--> $method $url, request body: ${String(body ?: byteArrayOf())}")
        // getting headers from encoders
        val allHeaders = encoders
            .fold(
                initial = mapOf<String, List<String>>(),
                operation = { headerMap, encoder ->
                    headerMap + encoder.getHeaders()
                }
            )
            .mergeHeadersWith(other = headers)
            .toMutableMap()

        // creating and adding signature for response verifying if needed
        val verifier = Verifier.newInstance()
        if (useUniqueRequestId && !url.startsWith(BaseAppodealUrl)) {
            val uniqueRequestId = verifier.createRequestId()
            allHeaders[XRequestId] = listOf(uniqueRequestId)
        }

        // encoding data
        val requestBody = body?.encodeWith(encoders) ?: byteArrayOf()

        val rawRequest = RawRequest(
            method = method,
            url = url,
            headers = allHeaders,
            body = requestBody
        )
        return rawRequestClient.execute(rawRequest)
            .mapCatching { rawResponse ->
                when (rawResponse) {
                    is RawResponse.Failure -> {
                        throw rawResponse.httpError
                    }
                    is RawResponse.Success -> {
                        // decoding data
                        // verifying response signature if needed
                        if (useUniqueRequestId && !url.startsWith(BaseAppodealUrl)) {
                            val signature = rawResponse.headers[XSignature]?.firstOrNull()
                            val isSuccessfullyVerified = verifier.isResponseValid(responseId = signature)
                            if (!isSuccessfullyVerified) {
                                throw HttpError.RequestVerificationFailed(rawResponse.data)
                            }
                        }
                        // decoding data
                        val data = rawResponse.data?.decodeWith(
                            contentEncoding = rawResponse.contentEncoding,
                            decoders = decoders
                        )
                        val result = try {
                            parser(data)?.also {
                                logInternal(TAG, "<-- ${rawRequest.method}     ${rawRequest.url}, decoded response: $it")
                            }
                        } catch (e: Exception) {
                            null
                        }
                        // success
                        result
                    }
                }
            }
    }

    private fun Map<String, List<String>>.mergeHeadersWith(other: Map<String, List<String>>): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>().also {
            it.putAll(this)
        }
        other.forEach { (key, values) ->
            if (result.containsKey(key)) {
                result[key] = ((result[key] ?: emptyList()) + values).distinct()
            } else {
                result[key] = values
            }
        }
        return result
    }
}

private const val XRequestId = "X-Request-ID"
private const val XSignature = "X-Signature"
private const val TAG = "HttpClientImpl"
