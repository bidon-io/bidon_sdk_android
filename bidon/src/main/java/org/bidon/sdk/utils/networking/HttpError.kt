package org.bidon.sdk.utils.networking

sealed class HttpError : Throwable() {
    abstract val description: String
    abstract val rawResponse: ByteArray?
    abstract val code: Int

    object InternalError : HttpError() {
        override val description: String = "internal error"
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    object RequestError : HttpError() {
        override val description: String = "request error"
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    object ServerError : HttpError() {
        override val description: String = "server error"
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    class UncaughtException(val error: Throwable) : HttpError() {
        override val description: String = ""
        override val rawResponse: ByteArray? = null
        override val code: Int = -1
    }
}