package org.bidon.sdk.config

import android.content.Context
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.utils.networking.NetworkSettings

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface BidonInitializer {

    val isInitialized: Boolean

    /**
     * Default adapters is in [DefaultAdapters]
     */
    fun registerDefaultAdapters()

    /**
     * Registering custom Adapters
     */
    fun registerAdapters(vararg adapters: Adapter)

    /**
     * Registering custom Adapters by full class name
     */
    fun registerAdapter(adaptersClassName: String)

    /**
     * Bidon SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    fun setInitializationCallback(initializationCallback: InitializationCallback)

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidonBaseUrl]
     */
    fun setBaseUrl(host: String)

    fun initialize(context: Context, appKey: String)
}
