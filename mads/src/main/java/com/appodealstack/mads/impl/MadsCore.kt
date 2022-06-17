package com.appodealstack.mads.impl

import android.annotation.SuppressLint
import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.Mediator
import com.appodealstack.mads.config.MadsConfigurator
import com.appodealstack.mads.config.MadsConfiguratorInstance
import com.appodealstack.mads.initializing.InitializationCallback
import com.appodealstack.mads.initializing.InitializationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object MadsCore : Mediator {
    private val demands = mutableListOf<Demand>()
    private val scope: CoroutineScope = GlobalScope
    private var context: Context? = null
    private val requiredContext: Context = requireNotNull(context) {
        "Context is not provided. Use [Mads.withContext()] before [Mads.build()]"
    }
    private val madsConfigurator: MadsConfigurator get() = MadsConfiguratorInstance

    override fun withContext(context: Context): Mediator {
        this.context = context.applicationContext
        return this
    }

    override fun registerAnalytics(vararg analytics: Analytic): Mediator {
        TODO("Not yet implemented")
    }

    override fun registerDemands(vararg demandClasses: Class<out Demand>): Mediator {
        demandClasses.mapNotNull { demandClass ->
            try {
                demandClass.newInstance()
            } catch (e: Exception) {
                // TODO collect errors for [InitializationCallback]
                null
            }
        }.also {
            demands.addAll(it)
        }
        return this
    }

    override fun withConfigurations(vararg configurations: Configuration): Mediator {
        madsConfigurator.addConfigurations(*configurations)
        return this
    }

    override suspend fun build(): InitializationResult {
        demands.forEach { demand ->
            demand.init(
                context = requiredContext,
                configParams = madsConfigurator.getDemandConfig(demand.demandId)
            )
        }
        return InitializationResult.Success
    }

    override fun build(initCallback: InitializationCallback) {
        scope.launch {
            initCallback.onFinished(
                result = build()
            )
        }
    }
}