package com.appodealstack.bidon.logs.logging.impl

import com.appodealstack.bidon.logs.logging.Logger

/**
 * Created by Aleksei Cherniaev on 10/02/2023.
 */
internal class LoggerImpl : Logger {

    override var loggerLevel: Logger.Level = Logger.Level.Off

    override fun setLogLevel(logLevel: Logger.Level) {
        this.loggerLevel = logLevel
    }
}