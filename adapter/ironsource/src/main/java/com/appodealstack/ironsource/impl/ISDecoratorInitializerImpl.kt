package com.appodealstack.ironsource.impl

import android.app.Activity
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.IronSourceAdapter
import com.appodealstack.ironsource.IronSourceParameters
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.sdk.InitializationListener

internal class ISDecoratorInitializerImpl : ISDecorator.Initializer {
    override fun register(adapterClass: Class<out Adapter<*>>, parameters: AdapterParameters): ISDecorator.Initializer {
        BidOnInitializer.registerAdapter(adapterClass, parameters)
        return this
    }

    override fun init(
        activity: Activity,
        appKey: String,
        listener: InitializationListener,
        adUnit: IronSource.AD_UNIT?
    ) {
        val initializationListener = InitializationListener {
            BidOnInitializer
                .withContext(activity.applicationContext)
                .registerAdapter(IronSourceAdapter::class.java, IronSourceParameters)
                .build {
                    listener.onInitializationComplete()
                }
        }
        if (adUnit == null) {
            IronSource.init(activity, appKey, initializationListener)
        } else {
            IronSource.init(activity, appKey, initializationListener, adUnit)
        }
    }
}