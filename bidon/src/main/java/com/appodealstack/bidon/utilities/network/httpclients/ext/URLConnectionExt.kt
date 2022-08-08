package com.appodealstack.bidon.utilities.network.httpclients.ext

import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.core.ext.toHexString
import com.appodealstack.bidon.utilities.network.HttpError
import com.appodealstack.bidon.utilities.network.httpclients.RawRequest
import com.appodealstack.bidon.utilities.network.httpclients.RawResponse
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLConnection

internal fun URLConnection.requestRawData(
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
            throw HttpError.RequestError
        } finally {
            writer?.flush()
            writer?.close()
        }
    }

    /**
     * Response <--
     */
    var responseInputStream: InputStream? = null
    var responseBytesOutputStream: ByteArrayOutputStream? = null
    try {
        val inputStream = connection.getInputStream().also {
            responseInputStream = it
        }
        val bytesOutputStream = ByteArrayOutputStream().also {
            responseBytesOutputStream = it
        }
        val responseCode = (connection as? HttpURLConnection)?.responseCode ?: -1
        val responseHeaders = connection.headerFields ?: mapOf()

        // obtaining raw response data
        val responseRawData = try {
            val buffer = ByteArray(BufferSize)
            var length = inputStream.read(buffer)
            while (length != EndOfStream) {
                bytesOutputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            val rawResponse = bytesOutputStream.toByteArray()
            logInternal("URLConnection", " <-- ${request.method} $responseCode ${connection.url}, raw response(size: ${rawResponse.size}, data: ${rawResponse.toHexString()})")
            rawResponse
        } catch (e: Exception) {
            null
        }
        val hasNoFill = responseCode in 201 until 300
        if (responseCode == HttpURLConnection.HTTP_OK || hasNoFill) {
            // success, HTTP 200 OK. NO_FILL - is success but without response body
            return@runCatching RawResponse.Success(
                code = responseCode,
                headers = responseHeaders,
                data = responseRawData,
                contentEncoding = connection.contentEncoding
            )
        }
        val error = when (responseCode) {
            in 400 until 500 -> HttpError.RequestError
            in 500 until 600 -> HttpError.ServerError
            else -> HttpError.InternalError
        }
        return@runCatching RawResponse.Failure(
            code = responseCode,
            headers = responseHeaders,
            response = responseRawData,
            httpError = error
        )
    } catch (e: Exception) {
        return@runCatching RawResponse.Failure(
            code = -1,
            headers = mapOf(),
            response = null,
            httpError = HttpError.UncaughtException(e)
        )
    } finally {
        responseInputStream?.close()
        responseBytesOutputStream?.flush()
        responseBytesOutputStream?.close()
    }
}

private const val DefaultConnectTimeoutMs = 40_000
private const val BufferSize = 1024
internal const val EndOfStream = -1
