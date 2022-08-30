package com.appodealstack.bidon.core.impl

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.analytics.AdRevenueLogger
import com.appodealstack.bidon.config.domain.BidOnInitializer
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.di.DI
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

/**
 * [DI.init] must be invoked before using all. Check if SDK is initialized with [isInitialized].
 */
internal class BidOnSdkImpl : BidOnSdk {

    private val bidONInitializerDelegate = lazy { bidONInitializer }
    private val bidONInitializer: BidOnInitializer by lazy { get() }

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher by lazy { newSingleThreadContext("BidON") }
    private val scope get() = CoroutineScope(dispatcher)
    private val adaptersSource: AdaptersSource by lazy { get() }
    private val bidOnEndpoints: BidOnEndpoints by lazy { get() }

    override fun isInitialized(): Boolean {
        return if (!bidONInitializerDelegate.isInitialized()) {
            false
        } else {
            bidONInitializer.isInitialized
        }
    }

    override fun init(activity: Activity, appKey: String, callback: InitializationCallback?) {
        DI.init(context = activity.applicationContext)
        scope.launch {
            runCatching {
                bidONInitializer.init(activity, appKey)
            }.onFailure {
                logError(message = "Error while initialization", error = it)
            }.onSuccess {
            }
            callback?.onFinished()
        }
    }

    override fun setBaseUrl(host: String?): BidOnSdk {
        host?.let {
            bidOnEndpoints.init(host, setOf())
        }
        return this
    }

    override fun logRevenue(ad: Ad) {
        adaptersSource.adapters.filterIsInstance<AdRevenueLogger>()
            .forEach { adRevenueLogger ->
                adRevenueLogger.logAdRevenue(ad)
            }
    }
}
