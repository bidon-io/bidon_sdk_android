package com.appodealstack.mads.core.ext

import android.util.Log
import com.appodealstack.mads.BuildConfig

fun logInternal(tag: String = DefaultTag, message: String, error: Throwable? = null) {
    if (BuildConfig.DEBUG) {
        val msg = if (tag == DefaultTag) message else "$tag: $message"
        val title = DefaultTag
        if (error != null) {
            Log.e(title, msg, error)
        } else {
            Log.d(title, msg)
        }
    }
}

private const val DefaultTag = "InternalLogs"