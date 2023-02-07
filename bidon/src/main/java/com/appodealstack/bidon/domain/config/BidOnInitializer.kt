package com.appodealstack.bidon.domain.config

import android.app.Activity
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface BidOnInitializer {
    val isInitialized: Boolean
    suspend fun init(activity: Activity, appKey: String): Result<Unit>
}
