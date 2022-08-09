package com.appodealstack.bidon.core

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext

@VisibleForTesting
var defaultDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
var ioDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
var singleDispatcherOverridden: CoroutineDispatcher? = null

object SdkDispatchers {
    val Single: CoroutineDispatcher get() = singleDispatcherOverridden ?: newSingleThreadContext("BidON")

    val Default: CoroutineDispatcher
        get() = defaultDispatcherOverridden ?: Dispatchers.Default

    val IO: CoroutineDispatcher
        get() = ioDispatcherOverridden ?: Dispatchers.IO
}