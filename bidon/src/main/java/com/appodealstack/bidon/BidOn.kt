package com.appodealstack.bidon

import android.app.Activity
import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.config.DefaultAdapters
import com.appodealstack.bidon.config.InitializationCallback
import com.appodealstack.bidon.config.impl.BidOnSdkImpl
import com.appodealstack.bidon.logs.logging.Logger
import com.appodealstack.bidon.utils.networking.NetworkSettings

/**
 * Created by Aleksei Cherniaev on 08/08/2022.
 */

object BidOn : BidOnSdk by BidOnSdkImpl()

/**
 * [BidOn] SDK API
 */
interface BidOnSdk : BidOnBuilder, Logger {
    companion object {
        const val DefaultPlacement = "default"
        const val DefaultMinPrice = 0.0
    }

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
    fun setDefaultAdapters(): BidOnBuilder

    /**
     * Registering custom Adapters
     */
    fun setAdapters(vararg adapters: Adapter): BidOnBuilder

    /**
     * Registering custom Adapters by full class name
     */
    fun setAdapters(adaptersClassName: String): BidOnBuilder

    /**
     * BidOn SDK always invokes [InitializationCallback.onFinished] callback.
     * If error occurs, it will be logged.
     */
    fun setInitializationCallback(initializationCallback: InitializationCallback): BidOnBuilder

    /**
     * Redefine BaseUrl for /action-requests. Default base url [NetworkSettings.BidOnBaseUrl]
     */
    fun setBaseUrl(host: String?): BidOnBuilder

    fun init(activity: Activity, appKey: String)
}