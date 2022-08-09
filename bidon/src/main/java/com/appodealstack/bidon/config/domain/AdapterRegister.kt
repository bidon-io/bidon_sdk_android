package com.appodealstack.bidon.config.domain

import android.app.Activity
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters

internal interface AdapterRegister {
    fun withContext(activity: Activity): AdapterRegister

    fun registerAdapter(
        adapterClass: Class<out Adapter>,
        parameters: AdapterParameters?
    ): AdapterRegister

    suspend fun build()
}