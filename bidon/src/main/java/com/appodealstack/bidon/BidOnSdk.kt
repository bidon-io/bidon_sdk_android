package com.appodealstack.bidon

import android.app.Activity
import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.config.DefaultAdapters
import com.appodealstack.bidon.config.InitializationCallback
import com.appodealstack.bidon.config.impl.BidOnImpl
import com.appodealstack.bidon.logs.logging.Logger
import com.appodealstack.bidon.utils.networking.NetworkSettings

/**
 * Created by Aleksei Cherniaev on 08/08/2022.
 */

object BidOnSdk : BidOn by BidOnImpl() {
    const val DefaultPlacement = "default"
    const val DefaultPricefloor = 0.0
    const val SdkVersion = BuildConfig.ADAPTER_VERSION
}

/**
 * [BidOnSdk] SDK API
 */
interface BidOn : BidOnBuilder, Logger {
    override fun setLogLevel(logLevel: Logger.Level)
    fun isInitialized(): Boolean
}

/**
 * Initialization builder
 */
interface BidOnBuilder {
    /**
     * Default adapters is in [DefaultAdapters]
     */
    fun registerDefaultAdapters(): BidOnBuilder

    /**
     * Registering custom Adapters
     */
    fun registerAdapters(vararg adapters: Adapter): BidOnBuilder

    /**
     * Registering custom Adapters by full class name
     */
    fun registerAdapters(adaptersClassName: String): BidOnBuilder

    /**
     * BidOn SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    fun setInitializationCallback(initializationCallback: InitializationCallback): BidOnBuilder

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidOnBaseUrl]
     */
    fun setBaseUrl(host: String): BidOnBuilder

    fun initialize(activity: Activity, appKey: String)
}