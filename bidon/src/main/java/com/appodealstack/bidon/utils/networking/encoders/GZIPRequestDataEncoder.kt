package com.appodealstack.bidon.utils.networking.encoders

import com.appodealstack.bidon.utils.networking.impl.EndOfStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal object GZIPRequestDataEncoder : RequestDataEncoder, RequestDataDecoder {
    override fun getHeaders(): Map<String, List<String>> = mapOf(
        "Accept-Encoding" to listOf("gzip"),
        "Content-Encoding" to listOf("gzip"),
    )

    override fun encode(data: ByteArray): ByteArray {
        var osBytes: ByteArrayOutputStream? = null
        var osGzip: GZIPOutputStream? = null
        return try {
            osBytes = ByteArrayOutputStream()
            osGzip = GZIPOutputStream(osBytes)
            osGzip.write(data)
            // required for write all pending bytes
            osGzip.close()
            osGzip = null
            osBytes.toByteArray()
        } catch (e: Exception) {
            data
        } finally {
            osBytes?.close()
            osGzip?.close()
        }
    }

    override fun decode(contentEncoding: String?, data: ByteArray): ByteArray {
        if ("gzip" == contentEncoding) {
            var osBytes: ByteArrayOutputStream? = null
            var isBytes: ByteArrayInputStream? = null
            var isGzip: GZIPInputStream? = null
            return try {
                osBytes = ByteArrayOutputStream()
                isBytes = ByteArrayInputStream(data)
                isGzip = GZIPInputStream(isBytes)
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (isGzip.read(buffer).also { bytesRead = it } != EndOfStream) {
                    osBytes.write(buffer, 0, bytesRead)
                }
                osBytes.toByteArray()
            } finally {
                osBytes?.close()
                isBytes?.close()
                isGzip?.close()
            }
        }
        return data
    }
}