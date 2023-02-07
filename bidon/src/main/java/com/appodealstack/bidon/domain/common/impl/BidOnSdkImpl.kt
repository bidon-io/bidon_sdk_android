package com.appodealstack.bidon.domain.common.impl

import android.app.Activity
import com.appodealstack.bidon.BidOnBuilder
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.data.networking.BidOnEndpoints
import com.appodealstack.bidon.di.DI
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import com.appodealstack.bidon.domain.analytic.AdRevenueLogger
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.SdkState
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.InitializationCallback
import com.appodealstack.bidon.domain.stats.impl.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOnSdkImpl : BidOnSdk {

    private val bidOnInitializerDelegate = lazy { bidOnInitializer }
    private val bidOnInitializer: BidOnInitializer by lazy { get() }
    private val initializationState = MutableStateFlow(SdkState.NotInitialized)

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher by lazy { newSingleThreadContext("BidON") }
    private val scope get() = CoroutineScope(dispatcher)
    private val adaptersSource: AdaptersSource by lazy { get() }
    private val bidOnEndpoints: BidOnEndpoints by lazy { get() }
    private var initializationCallback: InitializationCallback? = null

    override fun isInitialized(): Boolean {
        return !bidOnInitializerDelegate.isInitialized() && bidOnInitializer.isInitialized
    }

    override fun setDefaultAdapters(): BidOnBuilder {
        if (initializationState.value == SdkState.NotInitialized) {
            bidOnInitializer.withDefaultAdapters()
        }
        return this
    }

    override fun setAdapters(vararg adapters: Adapter): BidOnBuilder {
        if (initializationState.value == SdkState.NotInitialized) {
            bidOnInitializer.withAdapters(*adapters)
        }
        return this
    }

    override fun setInitializationCallback(initializationCallback: InitializationCallback): BidOnBuilder {
        this.initializationCallback = initializationCallback
        return this
    }

    override fun setBaseUrl(host: String?): BidOnBuilder {
        host?.let {
            bidOnEndpoints.init(host, setOf())
        }
        return this
    }

    override fun init(activity: Activity, appKey: String) {
        /**
         * [DI.init] must be invoked before using all. Check if SDK is initialized with [isInitialized].
         */
        val isNotInitialized = initializationState.compareAndSet(
            expect = SdkState.NotInitialized,
            update = SdkState.Initializing
        )
        if (isNotInitialized) {
            DI.init(context = activity.applicationContext)
            scope.launch {
                runCatching {
                    bidOnInitializer.init(activity, appKey)
                }.onFailure {
                    logError(message = "Error while initialization", error = it)
                    initializationState.value = SdkState.InitializationFailed
                }.onSuccess {
                    initializationState.value = SdkState.Initialized
                }
                initializationCallback?.onFinished()
                initializationCallback = null
            }
        }
    }

    override fun logRevenue(ad: Ad) {
        adaptersSource.adapters.filterIsInstance<AdRevenueLogger>()
            .forEach { adRevenueLogger ->
                adRevenueLogger.logAdRevenue(ad)
            }
    }
}
