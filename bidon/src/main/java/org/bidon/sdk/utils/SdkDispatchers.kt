package org.bidon.sdk.utils

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext

/**
 * Created by Bidon Team on 06/02/2023.
 */

@VisibleForTesting
var defaultDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
var ioDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
var singleDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
var mainDispatcherOverridden: CoroutineDispatcher? = null

object SdkDispatchers {
    @OptIn(DelicateCoroutinesApi::class)
    val Single: CoroutineDispatcher
        get() = singleDispatcherOverridden ?: newSingleThreadContext("Bidon")

    val Main: CoroutineDispatcher
        get() = mainDispatcherOverridden ?: Dispatchers.Main

    val Default: CoroutineDispatcher
        get() = defaultDispatcherOverridden ?: Dispatchers.Default

    val IO: CoroutineDispatcher
        get() = ioDispatcherOverridden ?: Dispatchers.IO
}
