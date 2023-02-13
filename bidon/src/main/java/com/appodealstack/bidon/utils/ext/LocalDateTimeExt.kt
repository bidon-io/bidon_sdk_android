package com.appodealstack.bidon.utils.time

import android.os.SystemClock

internal val SystemTimeNow
    get() = System.currentTimeMillis()

internal val ElapsedMonotonicTimeNow
    get() = SystemClock.elapsedRealtime()