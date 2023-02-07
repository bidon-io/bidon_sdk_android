package com.appodealstack.bidon.domain.config

import android.app.Activity
import com.appodealstack.bidon.domain.adapter.Adapter

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface BidOnInitializer {
    val isInitialized: Boolean
    fun withDefaultAdapters()
    fun withAdapters(vararg adapters: Adapter)
    suspend fun init(activity: Activity, appKey: String): Result<Unit>
}
