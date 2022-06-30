package com.appodealstack.applovin

import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.mads.BidOnInitializer

object AppLovinSdkWrapper {
    fun getInstance(context: Context): AppLovinSdk {
        return AppLovinSdk.getInstance(context)
    }

    @JvmOverloads
    fun initializeSdk(context: Context, listener: AppLovinSdk.SdkInitializationListener? = null) {
        AppLovinSdk.initializeSdk(context) { appLovinSdkConfiguration ->
            BidOnInitializer.withContext(context)
                .registerDemands(ApplovinMaxDemand::class.java)
                .build {
                    listener?.onSdkInitialized(appLovinSdkConfiguration)
                }
        }
    }
}