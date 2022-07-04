package com.appodealstack.applovin

import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.demands.Demand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLovinSdkWrapper {

    fun getInstance(context: Context): AppLovinSdk {
        return AppLovinSdk.getInstance(context)
    }

    fun registerPostBidDemands(vararg demandClasses: Class<out Demand>): AppLovinSdkWrapper {
        BidOnInitializer.registerDemands(*demandClasses)
        return this
    }

    @JvmOverloads
    fun initializeSdk(context: Context, listener: AppLovinSdk.SdkInitializationListener? = null) {
        AppLovinSdk.initializeSdk(context) { appLovinSdkConfiguration ->
            BidOnInitializer.withContext(context)
                .registerDemands(
                    ApplovinMaxDemand::class.java,
                )
                .build {
                    CoroutineScope(Dispatchers.Main).launch {
                        listener?.onSdkInitialized(appLovinSdkConfiguration)
                    }
                }
        }
    }
}