package com.appodealstack.fyber

import android.app.Activity
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters

object FairBidDecorator {
    fun register(adapterClass: Class<out Adapter<*>>, parameters: AdapterParameters): FairBidDecorator {
        BidOnInitializer.registerAdapter(adapterClass, parameters)
        return this
    }

    fun start(appId: String, activity: Activity, onInitialized: () -> Unit) {
        BidOnInitializer
            .withContext(activity)
            .registerAdapter(FairBidAdapter::class.java, FairBidParameters(appId))
            .build {
                onInitialized.invoke()
            }
    }
}