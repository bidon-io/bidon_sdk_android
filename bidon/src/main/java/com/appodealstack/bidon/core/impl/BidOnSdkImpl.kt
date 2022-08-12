package com.appodealstack.bidon.core.impl

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.config.domain.BidOnInitializer
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.network.BidOnEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

internal class BidOnSdkImpl(
    private val bidONInitializer: BidOnInitializer,
    private val adaptersSource: AdaptersSource,
    private val contextProvider: ContextProvider,
    private val bidOnEndpoints: BidOnEndpoints,
) : BidOnSdk {

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher by lazy { newSingleThreadContext("BidON") }
    private val scope get() = CoroutineScope(dispatcher)

    override fun init(activity: Activity, appKey: String, callback: InitializationCallback?) {
        contextProvider.setContext(activity)
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

    override fun isInitialized(): Boolean = bidONInitializer.isInitialized

    override fun setBaseUrl(host: String?): BidOnSdk {
        host?.let {
            bidOnEndpoints.init(host, setOf())
        }
        return this
    }

}