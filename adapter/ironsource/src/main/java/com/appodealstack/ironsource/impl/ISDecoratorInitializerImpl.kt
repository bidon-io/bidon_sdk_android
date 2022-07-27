package com.appodealstack.ironsource.impl

import android.app.Activity
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.IronSourceAdapter
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.IronSourceParameters
import com.appodealstack.mads.BidOnInitializer
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticParameters
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.sdk.InitializationListener
import java.lang.ref.WeakReference

internal class ISDecoratorInitializerImpl : ISDecorator.Initializer {
    private var activityRef: WeakReference<Activity>? = null
    override val activity: Activity?
        get() = activityRef?.get()

    override fun register(adapterClass: Class<out Analytic<*>>, parameters: AnalyticParameters): ISDecorator.Initializer {
        BidOnInitializer.registerAnalytics(adapterClass, parameters)
        return this
    }

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
        activityRef = WeakReference(activity)
        BidOnInitializer
            .withContext(activity)
            .registerAdapter(IronSourceAdapter::class.java, IronSourceParameters(appKey, adUnit))
            .build {
                listener.onInitializationComplete()
                /**
                 * Original IronSource starts loading RewardedAd after init.
                 */
                IronSourceDecorator.loadRewardedVideo()
            }
    }
}