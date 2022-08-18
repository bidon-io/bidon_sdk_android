package com.appodealstack.bidon.core.errors

sealed class BidonSdkError(message: String?) : Throwable(message) {
    class AppKeyIsInvalid(message: String?) : BidonSdkError(message)
    class InternalServerSdkError(message: String?) : BidonSdkError(message)
    class UnknownError(description: String?, val sourceCause: Throwable? = null) : BidonSdkError(description ?: "Unknown Error")
}