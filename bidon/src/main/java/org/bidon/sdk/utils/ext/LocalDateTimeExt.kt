package org.bidon.sdk.utils.ext

import android.os.SystemClock
import androidx.annotation.VisibleForTesting

internal val SystemTimeNow
    get() = SystemTimeNowTestOnly ?: System.currentTimeMillis()

internal val ElapsedMonotonicTimeNow
    get() = ElapsedMonotonicTimeNowTestOnly ?: SystemClock.elapsedRealtime()

/**
 * Change system time for tests only
 */
@VisibleForTesting
internal var SystemTimeNowTestOnly: Long? = null

/**
 * Change system time for tests only
 */
@VisibleForTesting
internal var ElapsedMonotonicTimeNowTestOnly: Long? = null
