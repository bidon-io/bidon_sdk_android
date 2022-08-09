package com.appodealstack.bidon

import android.app.Activity
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.core.impl.SdkInitializationImpl
import com.appodealstack.bidon.demands.AdapterParameters

val BidOnInitializer: SdkInitialization by lazy { SdkInitializationImpl() }

interface SdkInitialization {
    fun withContext(activity: Activity): SdkInitialization

    fun registerAdapter(
        adapterClass: Class<out Adapter>,
        parameters: AdapterParameters?
    ): SdkInitialization

    suspend fun build()
    fun build(initCallback: InitializationCallback)
}