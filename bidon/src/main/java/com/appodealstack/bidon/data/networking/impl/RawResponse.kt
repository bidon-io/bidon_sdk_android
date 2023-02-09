package com.appodealstack.bidon.data.networking.impl

import com.appodealstack.bidon.data.networking.HttpError

internal sealed interface RawResponse {
    val headers: Map<String, List<String>>
    val code: Int

    class Success(
        override val headers: Map<String, List<String>>,
        override val code: Int,
        val contentEncoding: String?,
        val requestBody: ByteArray?,
    ) : RawResponse

    class Failure(
        override val headers: Map<String, List<String>>,
        override val code: Int,
        val httpError: HttpError,
        val responseBody: ByteArray?,
    ) : RawResponse
}