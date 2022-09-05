package com.appodealstack.bidon.core.ext

import android.os.SystemClock

val SystemTimeNow get() = System.currentTimeMillis()
val ElapsedMonotonicTimeNow get() = SystemClock.elapsedRealtime()