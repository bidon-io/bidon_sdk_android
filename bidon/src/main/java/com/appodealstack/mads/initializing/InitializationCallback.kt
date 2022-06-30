package com.appodealstack.mads.initializing

fun interface InitializationCallback {
    fun onFinished(result: InitializationResult)
}