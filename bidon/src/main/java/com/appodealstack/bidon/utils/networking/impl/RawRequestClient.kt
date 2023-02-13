package com.appodealstack.bidon.utils.networking.impl

import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.ext.toHexString
import com.appodealstack.bidon.utils.networking.HttpError
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
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

    private fun URLConnection.requestRawData(
        request: RawRequest,
        connectTimeout: Int = DefaultConnectTimeoutMs,
        readTimeout: Int = DefaultConnectTimeoutMs,
    ): Result<RawResponse> = runCatching {
        /**
         * Request -->
         */
        val connection = this.apply {
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
            (this as? HttpURLConnection)?.requestMethod = request.method.asRequestMethod()
        }
        request.headers.forEach { (key, values) ->
            connection.setRequestProperty(key, values.joinToString(separator = ";"))
        }
        // sending encoded data
        request.body?.let { requestBody ->
            connection.doOutput = true
            var writer: BufferedOutputStream? = null
            try {
                writer = BufferedOutputStream(connection.getOutputStream())
                writer.write(requestBody)
            } catch (e: java.lang.Exception) {
                throw e
            } finally {
                writer?.flush()
                writer?.close()
            }
        }

        /**
         * Response <--
         */
        try {
            val responseCode = (connection as? HttpURLConnection)?.responseCode ?: NoResponseCode
            val responseHeaders = connection.headerFields?.mapNotNull { (key, value) ->
                key.takeIf { !it.isNullOrBlank() }?.let { headerKey ->
                    headerKey to value.filterNotNull()
                }
            }?.toMap() ?: mapOf()
            if (responseCode in 200 until 400) {
                // success, HTTP 200 OK
                val rawResponse = connection.getResponseBody(streamType = StreamType.Normal)
                logResponseResult(
                    request = request,
                    responseCode = responseCode,
                    url = connection.url,
                    rawResponse = rawResponse
                )
                return@runCatching RawResponse.Success(
                    headers = responseHeaders,
                    code = responseCode,
                    requestBody = rawResponse,
                    contentEncoding = connection.contentEncoding
                )
            } else {
                // failed
                val rawResponse = connection.getResponseBody(streamType = StreamType.Error)
                logResponseResult(
                    request = request,
                    responseCode = responseCode,
                    url = connection.url,
                    rawResponse = rawResponse
                )
                return@runCatching RawResponse.Failure(
                    headers = responseHeaders,
                    code = responseCode,
                    responseBody = rawResponse,
                    httpError = when (responseCode) {
                        in 400 until 500 -> HttpError.RequestError
                        in 500 until 600 -> HttpError.ServerError
                        else -> HttpError.InternalError
                    }
                )
            }
        } catch (e: Exception) {
            return@runCatching RawResponse.Failure(
                headers = mapOf(),
                code = NoResponseCode,
                responseBody = null,
                httpError = HttpError.UncaughtException(e)
            )
        }
    }

    private fun logResponseResult(request: RawRequest, responseCode: Int, url: URL, rawResponse: ByteArray?) {
        logInfo(
            Tag,
            " <-- ${request.method} $responseCode $url, raw response(size: ${rawResponse?.size}, data: ${rawResponse?.toHexString()})"
        )
    }

    private fun URLConnection.getResponseBody(streamType: StreamType) = runCatching {
        // obtaining raw response data (bytes)
        var rawResponse: ByteArray? = null
        var responseInputStream: InputStream? = null
        var responseBytesOutputStream: ByteArrayOutputStream? = null
        try {
            val inputStream = when (streamType) {
                StreamType.Error -> (this as HttpURLConnection).errorStream
                StreamType.Normal -> (this as HttpURLConnection).inputStream
            }
            responseInputStream = inputStream
            val bytesOutputStream = ByteArrayOutputStream().also {
                responseBytesOutputStream = it
            }
            val buffer = ByteArray(BufferSize)
            var length = inputStream.read(buffer)
            while (length != EndOfStream) {
                bytesOutputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            rawResponse = bytesOutputStream.toByteArray()
        } catch (cause: Exception) {
            logError(Tag, "Error while obtaining data", cause)
        } finally {
            responseInputStream?.close()
            responseBytesOutputStream?.flush()
            responseBytesOutputStream?.close()
        }
        rawResponse
    }.getOrNull()

    private enum class StreamType {
        Normal,
        Error
    }
}

private const val DefaultConnectTimeoutMs = 40_000
private const val BufferSize = 1024
internal const val EndOfStream = -1
internal const val NoResponseCode = -1
private const val Tag = "RawRequestClient"
