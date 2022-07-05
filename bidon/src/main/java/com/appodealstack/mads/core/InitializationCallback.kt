package com.appodealstack.mads.core

fun interface InitializationCallback {
    fun onFinished(result: InitializationResult)
}