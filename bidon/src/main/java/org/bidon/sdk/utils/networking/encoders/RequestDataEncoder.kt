package org.bidon.sdk.utils.networking.encoders

internal interface RequestDataEncoder {
    fun getHeaders(): Map<String, List<String>>

    fun encode(
        data: ByteArray
    ): ByteArray
}

internal interface RequestDataDecoder {
    fun decode(
        contentEncoding: String?,
        data: ByteArray
    ): ByteArray
}
