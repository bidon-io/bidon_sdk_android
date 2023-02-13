package com.appodealstack.bidon.logs.logging

/**
 * Created by Aleksei Cherniaev on 10/02/2023.
 */
interface Logger {
    val loggerLevel: Level
    fun setLogLevel(logLevel: Level)

    enum class Level {
        Verbose, // all logs
        Error, // errors only
        Off, // no logs
    }
}