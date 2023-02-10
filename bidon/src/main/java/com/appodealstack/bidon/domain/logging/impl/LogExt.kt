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
fun logInfo(tag: String, message: String) {
    if (BidOn.loggerLevel in arrayOf(Level.Error, Level.Verbose)) {
        Log.d(DefaultTag, "[$tag] $message")
    }
}

fun logError(tag: String, message: String, error: Throwable?) {
    if (BidOn.loggerLevel == Level.Error) {
        Log.e(DefaultTag, "[$tag] $message", error)
    }
}

private const val DefaultTag = "BidOnLog"
