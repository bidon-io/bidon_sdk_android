package com.appodealstack.bidon.core.ext

fun <T : Any> T.asSuccess() = Result.success(this)
fun <T : Any> Throwable.asFailure() = Result.failure<T>(this)
