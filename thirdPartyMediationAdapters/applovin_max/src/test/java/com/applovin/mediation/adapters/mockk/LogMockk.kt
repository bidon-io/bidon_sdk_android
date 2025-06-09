package com.applovin.mediation.adapters.mockk

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

internal fun mockkLog() {
    mockkStatic(Log::class)
    every {
        Log.d(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        Log.e(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}, ${thirdArg<Throwable>()}")
        0
    }
    every {
        Log.d(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        Log.v(any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}")
        0
    }
    every {
        Log.v(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}, ${thirdArg<Throwable>()}")
        0
    }
    every {
        Log.v(any(), any(), any())
    } answers {
        println("[${firstArg<String>()}]: ${secondArg<String>()}, ${thirdArg<Throwable>()}")
        0
    }
}
