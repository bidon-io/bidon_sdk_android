package org.bidon.sdk.config

import android.app.Activity
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.utils.networking.NetworkSettings

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface BidOnInitializer {

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
     * BidOn SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    fun setInitializationCallback(initializationCallback: InitializationCallback)

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidOnBaseUrl]
     */
    fun setBaseUrl(host: String)

    fun initialize(activity: Activity, appKey: String)
}
