package org.bidon.sdk

import android.app.Activity
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.config.InitializationCallback
import org.bidon.sdk.config.impl.Bidon
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.networking.NetworkSettings

object BidonSdk {
    const val DefaultPlacement = "default"
    const val DefaultPricefloor = 0.0
    const val SdkVersion = BuildConfig.ADAPTER_VERSION

    private val bidon by lazy { Bidon() }

    @JvmStatic
    val loggerLevel: Logger.Level
        get() = bidon.loggerLevel

    @JvmStatic
    fun isInitialized(): Boolean = bidon.isInitialized

    @JvmStatic
    fun setLoggerLevel(logLevel: Logger.Level): BidonSdk {
        bidon.setLogLevel(logLevel)
        return this
    }

    /**
     * Default adapters is in [DefaultAdapters]
     */
    @JvmStatic
    fun registerDefaultAdapters(): BidonSdk {
        bidon.registerDefaultAdapters()
        return this
    }

    /**
     * Registering custom Adapters
     */
    @JvmStatic
    fun registerAdapters(vararg adapters: Adapter): BidonSdk {
        bidon.registerAdapters(*adapters)
        return this
    }

    /**
     * Registering custom Adapters by full class name
     */
    @JvmStatic
    fun registerAdapter(adaptersClassName: String): BidonSdk {
        bidon.registerAdapter(adaptersClassName)
        return this
    }

    /**
     * Bidon SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    @JvmStatic
    fun setInitializationCallback(initializationCallback: InitializationCallback): BidonSdk {
        bidon.setInitializationCallback(initializationCallback)
        return this
    }

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidonBaseUrl]
     */
    @JvmStatic
    fun setBaseUrl(host: String): BidonSdk {
        bidon.setBaseUrl(host)
        return this
    }

    @JvmStatic
    fun initialize(activity: Activity, appKey: String) = bidon.initialize(activity, appKey)
}
