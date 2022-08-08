package com.appodealstack.bidon.utilities.network.encoders

import android.util.Base64

internal object Base64RequestDataEncoder : RequestDataEncoder, RequestDataDecoder {
    override fun getHeaders(): Map<String, List<String>> = mapOf()

    override fun encode(data: ByteArray): ByteArray {
        return Base64.encode(data, Base64.DEFAULT)
    }

    override fun decode(contentEncoding: String?, data: ByteArray): ByteArray {
        return Base64.decode(data, Base64.DEFAULT)
    }
}