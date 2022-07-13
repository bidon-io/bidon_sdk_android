package com.appodealstack.applovin

import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLovinDecorator {

    fun getInstance(context: Context): AppLovinSdk {
        return AppLovinSdk.getInstance(context)
    }

    fun register(adapterClass: Class<out Adapter<*>>, parameters: AdapterParameters): AppLovinDecorator {
        BidOnInitializer.registerAdapter(adapterClass, parameters)
        return this
    }

    @JvmOverloads
    fun initializeSdk(context: Context, listener: AppLovinSdk.SdkInitializationListener? = null) {
        AppLovinSdk.initializeSdk(context) { appLovinSdkConfiguration ->
            BidOnInitializer.withContext(context)
                .registerAdapter(ApplovinMaxAdapter::class.java, ApplovinParameters())
                .build {
                    CoroutineScope(Dispatchers.Main).launch {
                        listener?.onSdkInitialized(appLovinSdkConfiguration)
                    }
                }
        }
    }
}