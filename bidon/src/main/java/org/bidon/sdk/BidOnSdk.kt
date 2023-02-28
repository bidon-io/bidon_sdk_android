package org.bidon.sdk

import android.app.Activity
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.config.InitializationCallback
import org.bidon.sdk.config.impl.BidOn
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.networking.NetworkSettings

object BidOnSdk {
    const val DefaultPlacement = "default"
    const val DefaultPricefloor = 0.0
    const val SdkVersion = BuildConfig.ADAPTER_VERSION

    private val bidon by lazy { BidOn() }

    @JvmStatic
    val loggerLevel: Logger.Level
        get() = bidon.loggerLevel

    @JvmStatic
    fun isInitialized(): Boolean = bidon.isInitialized

    @JvmStatic
    fun setLoggerLevel(logLevel: Logger.Level): BidOnSdk {
        bidon.setLogLevel(logLevel)
        return this
    }

    /**
     * Default adapters is in [DefaultAdapters]
     */
    @JvmStatic
    fun registerDefaultAdapters(): BidOnSdk {
        bidon.registerDefaultAdapters()
        return this
    }

    /**
     * Registering custom Adapters
     */
    @JvmStatic
    fun registerAdapters(vararg adapters: Adapter): BidOnSdk {
        bidon.registerAdapters(*adapters)
        return this
    }

    /**
     * Registering custom Adapters by full class name
     */
    @JvmStatic
    fun registerAdapter(adaptersClassName: String): BidOnSdk {
        bidon.registerAdapter(adaptersClassName)
        return this
    }

    /**
     * BidOn SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    @JvmStatic
    fun setInitializationCallback(initializationCallback: InitializationCallback): BidOnSdk {
        bidon.setInitializationCallback(initializationCallback)
        return this
    }

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidOnBaseUrl]
     */
    @JvmStatic
    fun setBaseUrl(host: String): BidOnSdk {
        bidon.setBaseUrl(host)
        return this
    }

    @JvmStatic
    fun initialize(activity: Activity, appKey: String) = bidon.initialize(activity, appKey)
}
