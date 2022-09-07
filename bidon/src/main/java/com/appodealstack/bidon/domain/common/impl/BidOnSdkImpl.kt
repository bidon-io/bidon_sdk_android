package com.appodealstack.bidon.domain.common.impl

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.data.networking.BidOnEndpoints
import com.appodealstack.bidon.di.DI
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import com.appodealstack.bidon.domain.analytic.AdRevenueLogger
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.InitializationCallback
import com.appodealstack.bidon.domain.stats.impl.logError
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
        return !bidONInitializerDelegate.isInitialized() && bidONInitializer.isInitialized
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
