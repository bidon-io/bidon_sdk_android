package com.applovin.mediation.adapters.logger

import android.util.Log

internal interface Logger {
    fun log(tag: String, message: String)
    fun logError(tag: String, message: String, throwable: Throwable)
}

internal val AppLovinSdkLogger: Logger = object : Logger {
    override fun log(tag: String, message: String) {
        Log.d("AppLovinSdk", "[$tag] $message")
    }

    override fun logError(tag: String, message: String, throwable: Throwable) {
        Log.e("AppLovinSdk", "[$tag] $message", throwable)
    }
}
