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
internal var defaultDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
internal var ioDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
internal var singleDispatcherOverridden: CoroutineDispatcher? = null

@VisibleForTesting
internal var mainDispatcherOverridden: CoroutineDispatcher? = null

internal object SdkDispatchers {
    @OptIn(DelicateCoroutinesApi::class)
    val Bidon: CoroutineDispatcher
        get() = singleDispatcherOverridden ?: newSingleThreadContext("Bidon")

    val Main: CoroutineDispatcher
        get() = mainDispatcherOverridden ?: Dispatchers.Main

    val Default: CoroutineDispatcher
        get() = defaultDispatcherOverridden ?: Dispatchers.Default

    val IO: CoroutineDispatcher
        get() = ioDispatcherOverridden ?: Dispatchers.IO
}
