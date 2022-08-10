package com.appodealstack.bidon.core.impl

import android.app.Activity
import com.appodealstack.bidon.BidONSdk
import com.appodealstack.bidon.config.domain.BidONInitializer
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.core.ext.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

internal class BidONSdkImpl(
    private val bidONInitializer: BidONInitializer
) : BidONSdk {
    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher by lazy { newSingleThreadContext("BidON") }
    private val scope get() = CoroutineScope(dispatcher)

    override fun init(activity: Activity, appKey: String, callback: InitializationCallback?) {
        scope.launch {
            runCatching {
                bidONInitializer.init(activity, appKey)
            }.onFailure {
                logError(message = "Error while initialization", error = it)
            }
            callback?.onFinished()
        }
    }
}