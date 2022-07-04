package com.appodealstack.applovin

import android.content.Context
import com.applovin.sdk.AppLovinSdk
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.demands.Demand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLovinSdkWrapper {
    private val postBidDemands = mutableSetOf<Class<out Demand>>()

    fun getInstance(context: Context): AppLovinSdk {
        return AppLovinSdk.getInstance(context)
    }

    fun registerPostBidDemands(vararg demandClasses: Class<out Demand>): AppLovinSdkWrapper {
        postBidDemands.addAll(demandClasses)
        return this
    }

    @JvmOverloads
    fun initializeSdk(context: Context, listener: AppLovinSdk.SdkInitializationListener? = null) {
        AppLovinSdk.initializeSdk(context) { appLovinSdkConfiguration ->
            BidOnInitializer.withContext(context)
                .registerDemands(
                    ApplovinMaxDemand::class.java,
                    *postBidDemands.toTypedArray()
                )
                .build {
                    CoroutineScope(Dispatchers.Main).launch {
                        postBidDemands.clear()
                        listener?.onSdkInitialized(appLovinSdkConfiguration)
                    }
                }
        }
    }
}