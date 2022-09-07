package com.appodealstack.bidon.domain.config

import android.app.Activity

internal interface BidOnInitializer {
    val isInitialized: Boolean
    suspend fun init(activity: Activity, appKey: String): Result<Unit>
}
