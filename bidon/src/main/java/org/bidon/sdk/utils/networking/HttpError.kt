package org.bidon.sdk.utils.networking

sealed class HttpError : Throwable() {
    abstract override val cause: Throwable
    abstract val rawResponse: ByteArray?
    abstract val code: Int

    object InternalError : HttpError() {
        override val cause: Throwable = Throwable("internal error")
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    object RequestError : HttpError() {
        override val cause: Throwable = Throwable("request error")
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    object ServerError : HttpError() {
        override val cause: Throwable = Throwable("server error")
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    class UncaughtException(override val cause: Throwable) : HttpError() {
        override val rawResponse: ByteArray? = null
        override val code: Int = -1
    }
}