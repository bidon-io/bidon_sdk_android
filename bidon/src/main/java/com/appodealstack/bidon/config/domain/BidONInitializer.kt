package com.appodealstack.bidon.config.domain

import android.app.Activity

internal interface BidONInitializer {
    suspend fun init(activity: Activity, appKey: String): Result<Unit>
}