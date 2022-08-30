package com.appodealstack.bidon.utilities.network.httpclients

import com.appodealstack.bidon.utilities.network.HttpClient
import com.appodealstack.bidon.utilities.network.HttpError
import com.appodealstack.bidon.utilities.network.httpclients.ext.requestRawData
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

internal class RawRequestClient {
    fun execute(
        rawRequest: RawRequest
    ): Result<RawResponse> {
        var urlConnection: URLConnection? = null
        val url = rawRequest.url
        return try {
            val connection = URL(url).openConnection().also {
                urlConnection = it
            }
            connection.requestRawData(rawRequest)
        } finally {
            (urlConnection as? HttpURLConnection)?.disconnect()
        }
    }
}

internal class RawRequest(
    val method: HttpClient.Method,
    val url: String,
    val body: ByteArray?,
    val headers: Map<String, List<String>>,
)

internal sealed interface RawResponse {
    class Success(
        val code: Int,
        val data: ByteArray?,
        val contentEncoding: String?,
        val headers: Map<String, List<String>>,
    ) : RawResponse

    class Failure(
        val code: Int,
        val response: ByteArray?,
        val headers: Map<String, List<String>>,
        val httpError: HttpError,
    ) : RawResponse
}
