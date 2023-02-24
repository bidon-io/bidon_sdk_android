package org.bidon.sdk.utils.networking

import org.bidon.sdk.utils.networking.impl.RawResponse

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
