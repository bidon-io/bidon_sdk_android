package org.bidon.sdk.utils.networking.encoders.ext

import org.bidon.sdk.utils.networking.encoders.RequestDataDecoder
import org.bidon.sdk.utils.networking.encoders.RequestDataEncoder

internal fun ByteArray.decodeWith(
    contentEncoding: String?,
    decoders: List<RequestDataDecoder>
): ByteArray {
    var response = this
    decoders.forEach { decoder ->
        response = decoder.decode(contentEncoding, response)
    }
    return response
}

internal fun ByteArray.encodeWith(
    encoders: List<RequestDataEncoder>,
): ByteArray {
    var requestBody = this
    encoders.forEach { encoder ->
        requestBody = encoder.encode(requestBody)
    }
    return requestBody
}