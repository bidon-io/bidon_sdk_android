package org.bidon.sdk

import android.content.Context
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.config.InitializationCallback
import org.bidon.sdk.config.impl.Bidon
import org.bidon.sdk.databinders.app.UnitySpecificInfo
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.networking.NetworkSettings

/**
 * Created by Aleksei Cherniaev on 07/02/2023.
 */
object BidonSdk {
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
    fun initialize(context: Context, appKey: String) =
        bidon.initialize(context.applicationContext, appKey)

    /**
     * Adding SDK-level extra data.
     * @param key name of [Extras] data
     * @param value value of extra data. Null removes data if exists.
     *              Possible types are String, Int, Long, Double, Float, Boolean, Char
     */
    @JvmStatic
    fun addExtra(key: String, value: Any?): BidonSdk {
        bidon.addExtra(key, value)
        return this
    }

    /**
     * Obtaining SDK-level [Extras]
     */
    @JvmStatic
    fun getExtras(): Map<String, Any> = bidon.getExtras()

    /**
     * Unity uses only
     */
    @JvmStatic
    fun setFramework(framework: String): BidonSdk {
        UnitySpecificInfo.frameworkName = framework
        return this
    }

    /**
     * Unity uses only
     */
    @JvmStatic
    fun setFrameworkVersion(version: String): BidonSdk {
        UnitySpecificInfo.frameworkVersion = version
        return this
    }

    /**
     * Unity uses only
     */
    @JvmStatic
    fun setPluginVersion(version: String): BidonSdk {
        UnitySpecificInfo.pluginVersion = version
        return this
    }
}
