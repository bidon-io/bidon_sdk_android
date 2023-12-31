package org.bidon.sdk.logs.logging.impl

import org.bidon.sdk.logs.logging.Logger

/**
 * Created by Bidon Team on 10/02/2023.
 */
internal class LoggerImpl : Logger {

    override var loggerLevel: Logger.Level = Logger.Level.Off

    override fun setLogLevel(logLevel: Logger.Level) {
        this.loggerLevel = logLevel
    }
}