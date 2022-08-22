package com.appodealstack.bidon.utilities.network

interface Networking {
    fun <Response> enqueue(
        method: HttpClient.Method,
        url: String,
        body: ByteArray?,
        parser: (response: ByteArray?) -> Response?,
        useUniqueRequestId: Boolean = false,
    ): Result<Response?>
}
