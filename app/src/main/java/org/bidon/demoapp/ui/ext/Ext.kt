package org.bidon.demoapp.ui.ext

import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by Aleksei Cherniaev on 13/07/2023.
 */
internal val LocalDateTimeNow get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
} else {
    System.currentTimeMillis()
}