package com.appodealstack.bidon.domain.common.ext

fun <T : Any> T.asSuccess() = Result.success(this)
fun <T : Any> Throwable.asFailure() = Result.failure<T>(this)

inline fun <T> Result<T>.onAny(action: () -> Unit): Result<T> {
    action.invoke()
    return this
}
