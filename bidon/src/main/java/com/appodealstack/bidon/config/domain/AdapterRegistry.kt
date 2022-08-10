package com.appodealstack.bidon.config.domain

import android.app.Activity
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters

internal interface AdapterRegistry {
    fun withContext(activity: Activity): AdapterRegistry

    fun registerAdapter(
        adapterClass: Class<out Adapter>,
        parameters: AdapterParameters?
    ): AdapterRegistry

    suspend fun build()
}