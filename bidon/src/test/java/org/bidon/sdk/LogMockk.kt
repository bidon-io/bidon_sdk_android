package org.bidon.sdk

import io.mockk.every
import io.mockk.mockkStatic
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.ext.SystemTimeNow

internal fun freezeTime(time: Long = 1000L): Long {
    mockkStatic("org.bidon.sdk.utils.ext.LocalDateTimeExtKt")
    every { SystemTimeNow } returns time
    return time
}

internal fun mockkLog() {
//    mockkStatic(Log::class)
//    every { Log.log(any(), any()) }  answers {
//        println("[${firstArg<String>()}]: ${secondArg<String>()}")
//    }
//    every { Log.log(any(), any(), any<String>()) }  answers {
//        println("[${firstArg<String>()}]: ${secondArg<String>()}")
//    }
//    every { Log.log(any(), any(), any<LogLevel>()) }  answers {
//        println("[${firstArg<String>()}]: ${secondArg<String>()}")
//    }

    BidonSdk.setLoggerLevel(Logger.Level.Verbose)
    mockkStatic(android.util.Log::class)
    every {
        android.util.Log.d(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        android.util.Log.e(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        android.util.Log.d(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        android.util.Log.v(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        android.util.Log.v(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        android.util.Log.v(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
}