package org.bidon.sdk.logs.logging

/**
 * Created by Bidon Team on 10/02/2023.
 */
public interface Logger {
    public val loggerLevel: Level
    public fun setLogLevel(logLevel: Level)

    public enum class Level {
        Verbose, // all logs
        Error, // errors only
        Off, // no logs
    }
}