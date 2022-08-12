package com.appodealstack.bidon.core.errors

sealed class BidonError(message: String?) : Throwable(message) {
    class AppKeyIsInvalid(message: String?) : BidonError(message)
    class InternalServerError(message: String?) : BidonError(message)
    class UnknownError(description: String?, val sourceCause: Throwable? = null) : BidonError(description ?: "Unknown Error")
}