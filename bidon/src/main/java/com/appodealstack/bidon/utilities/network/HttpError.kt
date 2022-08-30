package com.appodealstack.bidon.utilities.network

sealed class HttpError : Throwable() {
    abstract val description: String
    abstract val rawResponse: ByteArray?
    abstract val code: Int

    object NoFill : HttpError() {
        override val description: String = "no fill"
        override val code: Int = 2
        override val rawResponse: ByteArray? = null
    }

    object InternalError : HttpError() {
        override val description: String = "internal error"
        override val code: Int = 4
        override val rawResponse: ByteArray? = null
    }

    object TimeoutError : HttpError() {
        override val description: String = "timeout error"
        override val code: Int = 3
        override val rawResponse: ByteArray? = null
    }

    class ConnectionError(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "connection error"
        override val code: Int = 4
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

    class RequestVerificationFailed(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "request verification failed"
        override val code: Int = 4
    }

    class SdkVersionNotSupported(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "sdk version not supported"
        override val code: Int = 4
    }

    class InvalidAssets(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "invalid assets"
        override val code: Int = 7
    }

    class AdapterNotFound(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "adapter not found"
        override val code: Int = 8
    }

    class AdTypeNotSupportedInAdapter(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "ad type not supported in adapter"
        override val code: Int = 9
    }

    class Canceled(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "ad request canceled"
        override val code: Int = 2
    }

    class IncorrectAdUnit(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "incorrect adunit"
        override val code: Int = 2
    }

    class IncorrectCreative(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "incorrect creative"
        override val code: Int = 4
    }

    class ShowFailed(override val rawResponse: ByteArray?) : HttpError() {
        override val description: String = "show failed"
        override val code: Int = 4
    }

    class UncaughtException(val error: Throwable) : HttpError() {
        override val description: String = ""
        override val rawResponse: ByteArray? = null
        override val code: Int = -1
    }
}
