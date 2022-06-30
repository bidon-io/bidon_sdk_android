package com.appodealstack.mads.impl

import android.annotation.SuppressLint
import android.content.Context
import com.appodealstack.mads.BidOnCore
import com.appodealstack.mads.BidOnInitialization
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.config.MadsConfigurator
import com.appodealstack.mads.config.MadsConfiguratorInstance
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandsSource
import com.appodealstack.mads.initializing.InitializationCallback
import com.appodealstack.mads.initializing.InitializationResult
import com.appodealstack.mads.postbid.AdmobDemand
import com.appodealstack.mads.postbid.BidMachineDemand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
internal class BidOnInitializationImpl : BidOnInitialization {
    private val sdkCore: BidOnCore = SdkCore
    private val demands = mutableMapOf<Class<out Demand>, Demand>()
    private val analytics = mutableMapOf<Class<out Analytic>, Analytic>()
    private val scope: CoroutineScope get() = GlobalScope
    private var context: Context? = null
    private val requiredContext: Context
        get() = requireNotNull(context) {
            "Context is not provided. Use [Mads.withContext()] before [Mads.build()]"
        }
    private val madsConfigurator: MadsConfigurator get() = MadsConfiguratorInstance

    override fun withContext(context: Context): BidOnInitialization {
        this.context = context.applicationContext
        return this
    }

    override fun withConfigurations(vararg configurations: Configuration): BidOnInitialization {
        madsConfigurator.addConfigurations(*configurations)
        return this
    }

    override fun registerDemands(vararg demandClasses: Class<out Demand>): BidOnInitialization {
        demandClasses.forEach { demandClass ->
            try {
                val instance = demandClass.newInstance()
                demands[demandClass] = instance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    override fun registerAnalytics(vararg analyticsClasses: Class<out Analytic>): BidOnInitialization {
        analyticsClasses.forEach { analyticsClass ->
            try {
                val instance = analyticsClass.newInstance()
                analytics[analyticsClass] = instance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    override suspend fun build(): InitializationResult {
        registerPostBidDemands()
        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, demand) ->
            demand.init(
                context = requiredContext,
                configParams = madsConfigurator.getDemandConfig(demand.demandId)
            )
            sdkCore.addDemands(demand)
        }
        demands.clear()

        // Init Analytics
        require(sdkCore is AnalyticsSource)
        analytics.forEach { (_, analytics) ->
            analytics.init(
                context = requiredContext,
                configParams = madsConfigurator.getServiceConfig(analytics.analyticsId)
            )
            sdkCore.addAnalytics(analytics)
        }
        analytics.clear()
        return InitializationResult.Success
    }

    override fun build(initCallback: InitializationCallback) {
        scope.launch {
            initCallback.onFinished(
                result = build()
            )
        }
    }

    private fun registerPostBidDemands() {
        withConfigurations(

        )
        registerDemands(
            AdmobDemand::class.java,
            BidMachineDemand::class.java
        )
    }
}