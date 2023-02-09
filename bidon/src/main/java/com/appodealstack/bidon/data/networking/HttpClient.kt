package com.appodealstack.bidon.data.networking

import com.appodealstack.bidon.data.networking.impl.RawResponse

internal interface HttpClient {
    suspend fun enqueue(
        method: Method,
        url: String,
        body: ByteArray?,
    ): Result<RawResponse>
}

internal enum class Method {
    GET, POST, PUT, DELETE
}
