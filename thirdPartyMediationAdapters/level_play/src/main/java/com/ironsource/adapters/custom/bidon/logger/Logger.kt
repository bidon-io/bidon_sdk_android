package com.ironsource.adapters.custom.bidon.logger

import android.util.Log

internal interface Logger {
    fun log(tag: String, message: String)
    fun logError(tag: String, message: String, throwable: Throwable)
}

internal val LevelPLaySdkLogger: Logger = object : Logger {
    override fun log(tag: String, message: String) {
        Log.d("LevelPlaySdk", "[$tag] $message")
    }

    override fun logError(tag: String, message: String, throwable: Throwable) {
        Log.e("LevelPlaySdk", "[$tag] $message", throwable)
    }
}
