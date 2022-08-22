package com.appodealstack.bidon.core.ext

import android.util.Log
import com.appodealstack.bidon.BuildConfig

/**
 * Log only while debugging SDK
 */
fun logInternal(tag: String = DefaultTag, message: String, error: Throwable? = null) {
    if (BuildConfig.DEBUG) {
        val msg = if (tag == DefaultTag) message else "[$tag] $message"
        val title = DefaultTag
        if (error != null) {
            Log.e(title, msg, error)
        } else {
            Log.d(title, msg)
        }
    }
}

fun logError(tag: String = DefaultTag, message: String, error: Throwable? = null) {
    val msg = if (tag == DefaultTag) message else "[$tag] $message"
    val title = DefaultTag
    if (error != null) {
        Log.e(title, msg, error)
    } else {
        Log.d(title, msg)
    }
}

fun logInfo(tag: String = DefaultTag, message: String) {
    val msg = if (tag == DefaultTag) message else "[$tag] $message"
    val title = DefaultTag
    Log.d(title, msg)
}

private const val DefaultTag = "BidOn_log"
