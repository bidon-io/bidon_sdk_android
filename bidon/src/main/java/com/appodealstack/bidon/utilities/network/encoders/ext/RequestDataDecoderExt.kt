package com.appodealstack.bidon.utilities.network.encoders.ext

import com.appodealstack.bidon.utilities.network.encoders.RequestDataDecoder
import com.appodealstack.bidon.utilities.network.encoders.RequestDataEncoder

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