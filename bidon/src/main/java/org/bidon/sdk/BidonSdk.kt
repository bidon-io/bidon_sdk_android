package org.bidon.sdk

import android.content.Context
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.config.InitializationCallback
import org.bidon.sdk.config.impl.Bidon
import org.bidon.sdk.databinders.app.UnitySpecificInfo
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.segment.Segment
import org.bidon.sdk.utils.networking.NetworkSettings

/**
 * Created by Aleksei Cherniaev on 07/02/2023.
 */
public object BidonSdk {

    public const val DefaultPricefloor: Double = 0.0
    public const val SdkVersion: String = BuildConfig.BIDON_SDK_VERSION

    internal val bidon by lazy { Bidon() }

    /**
     * Represents User's [Segment]
     */
    @JvmStatic
    public val segment: Segment
        get() = bidon.segment

    @JvmStatic
    public val loggerLevel: Logger.Level
        get() = bidon.loggerLevel

    @JvmStatic
    public val regulation: Regulation
        get() = bidon.regulation

    @JvmStatic
    public val baseUrl: String
        get() = bidon.baseUrl

    @JvmStatic
    public val isTestMode: Boolean
        get() = bidon.isTestMode

    @JvmStatic
    public fun isInitialized(): Boolean = bidon.isInitialized

    @JvmStatic
    public fun setLoggerLevel(logLevel: Logger.Level): BidonSdk {
        bidon.setLogLevel(logLevel)
        return this
    }

    /**
     * Default adapters is in [DefaultAdapters]
     */
    @JvmStatic
    public fun registerDefaultAdapters(): BidonSdk {
        bidon.registerDefaultAdapters()
        return this
    }

    /**
     * Registering custom Adapters
     */
    @JvmStatic
    public fun registerAdapters(vararg adapters: Adapter): BidonSdk {
        bidon.registerAdapters(*adapters)
        return this
    }

    /**
     * Registering custom Adapters by full class name
     */
    @JvmStatic
    public fun registerAdapter(adaptersClassName: String): BidonSdk {
        bidon.registerAdapter(adaptersClassName)
        return this
    }

    /**
     * Bidon SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    @JvmStatic
    public fun setInitializationCallback(initializationCallback: InitializationCallback): BidonSdk {
        bidon.setInitializationCallback(initializationCallback)
        return this
    }

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidonBaseUrl]
     */
    @JvmStatic
    public fun setBaseUrl(host: String): BidonSdk {
        bidon.setBaseUrl(host)
        return this
    }

    @JvmStatic
    public fun initialize(context: Context, appKey: String): Unit =
        bidon.initialize(context.applicationContext, appKey)

    /**
     * Adding SDK-level extra data.
     * @param key name of [Extras] data
     * @param value value of extra data. Null removes data if exists.
     *              Possible types are String, Int, Long, Double, Float, Boolean, Char
     */
    @JvmStatic
    public fun addExtra(key: String, value: Any?): BidonSdk {
        bidon.addExtra(key, value)
        return this
    }

    /**
     * Obtaining SDK-level [Extras]
     */
    @JvmStatic
    public fun getExtras(): Map<String, Any> = bidon.getExtras()

    /**
     * Enabling test mode.
     * In test mode test ads will be shown and debug data will be written to logcat.
     *
     * @param isTestMode true to enable test mode. False by default.
     */
    @JvmStatic
    public fun setTestMode(isTestMode: Boolean): BidonSdk {
        bidon.isTestMode = isTestMode
        return this
    }

    /**
     * Unity uses only
     */
    @JvmStatic
    public fun setFramework(framework: String): BidonSdk {
        UnitySpecificInfo.frameworkName = framework
        return this
    }

    /**
     * Unity uses only
     */
    @JvmStatic
    public fun setFrameworkVersion(version: String): BidonSdk {
        UnitySpecificInfo.frameworkVersion = version
        return this
    }

    /**
     * Unity uses only
     */
    @JvmStatic
    public fun setPluginVersion(version: String): BidonSdk {
        UnitySpecificInfo.pluginVersion = version
        return this
    }
}
