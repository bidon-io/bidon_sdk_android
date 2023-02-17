package com.appodealstack.bidon.logs.logging.impl

import android.util.Log
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.logs.logging.Logger
import com.appodealstack.bidon.logs.logging.Logger.Level

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Set log level with [Logger]
 */
fun logInfo(tag: String, message: String) {
    if (BidOnSdk.loggerLevel == Level.Verbose) {
        Log.d(DefaultTag, "[$tag] $message")
    }
}

fun logError(tag: String, message: String, error: Throwable?) {
    if (BidOnSdk.loggerLevel in arrayOf(Level.Error, Level.Verbose)) {
        Log.e(DefaultTag, "[$tag] $message", error)
    }
}

private const val DefaultTag = "BidOnLog"
