package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.bidon.BidOnInitializer
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLovinDecorator {

    fun getInstance(context: Context): AppLovinSdk {
        return AppLovinSdk.getInstance(context)
    }

    fun register(adapterClass: Class<out Adapter>, parameters: AdapterParameters): AppLovinDecorator {
        BidOnInitializer.registerAdapter(adapterClass, parameters)
        return this
    }

    @JvmOverloads
    fun initializeSdk(context: Context, listener: AppLovinSdk.SdkInitializationListener? = null) {
        AppLovinSdk.initializeSdk(context) { appLovinSdkConfiguration ->
            BidOnInitializer.withContext(context as Activity)
                .registerAdapter(ApplovinMaxAdapter::class.java, ApplovinParameters())
                .build {
                    CoroutineScope(Dispatchers.Main).launch {
                        listener?.onSdkInitialized(appLovinSdkConfiguration)
                    }
                }
        }
    }
}