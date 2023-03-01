package org.bidon.sdk.logs.logging.impl

import android.util.Log
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.Logger.Level

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Set log level with [Logger]
 */
fun logInfo(tag: String, message: String) {
    if (BidonSdk.loggerLevel == Level.Verbose) {
        Log.d(DefaultTag, "[$tag] $message")
    }
}

fun logError(tag: String, message: String, error: Throwable?) {
    if (BidonSdk.loggerLevel in arrayOf(Level.Error, Level.Verbose)) {
        Log.e(DefaultTag, "[$tag] $message", error)
    }
}

private const val DefaultTag = "BidonLog"
