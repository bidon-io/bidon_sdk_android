package com.appodealstack.bidon.domain.logging.impl

import android.util.Log
import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.domain.logging.Logger
import com.appodealstack.bidon.domain.logging.Logger.Level

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Set log level with [Logger]
 */
fun logInfo(tag: String, message: String, error: Throwable? = null) {
    if (BidOn.loggerLevel in arrayOf(Level.Error, Level.Verbose)) {
        val msg = "[$tag] $message"
        val title = DefaultTag
        if (error != null) {
            Log.e(title, msg, error)
        } else {
            Log.d(title, msg)
        }
    }
}

fun logError(tag: String, message: String, error: Throwable?) {
    if (BidOn.loggerLevel in arrayOf(Level.Error)) {
        val msg = if (tag == DefaultTag) message else "[$tag] $message"
        val title = DefaultTag
        Log.e(title, msg, error)
    }
}

private const val DefaultTag = "BidOnLog"
