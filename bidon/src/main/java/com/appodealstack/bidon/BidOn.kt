package com.appodealstack.bidon

import android.app.Activity
import com.appodealstack.bidon.data.networking.NetworkSettings
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.impl.BidOnSdkImpl
import com.appodealstack.bidon.domain.config.DefaultAdapters
import com.appodealstack.bidon.domain.config.InitializationCallback

/**
 * Created by Aleksei Cherniaev on 08/08/2022.
 */

object BidOn : BidOnSdk by BidOnSdkImpl()

/**
 * [BidOn] SDK API
 */
interface BidOnSdk : BidOnBuilder {
    companion object {
        const val DefaultPlacement = "default"
        const val DefaultMinPrice = 0.0
    }

    fun isInitialized(): Boolean
    fun logRevenue(ad: Ad)
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