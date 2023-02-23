package org.bidon.sdk.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import org.bidon.sdk.utils.defaultDispatcherOverridden
import org.bidon.sdk.utils.ioDispatcherOverridden
import org.bidon.sdk.utils.mainDispatcherOverridden
import org.bidon.sdk.utils.singleDispatcherOverridden
import org.junit.After
import org.junit.Before

/**
 * [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
 *
 *
 *
 */
@Suppress("OPT_IN_USAGE")
abstract class ConcurrentTest {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        defaultDispatcherOverridden = mainThreadSurrogate
        ioDispatcherOverridden = mainThreadSurrogate
        singleDispatcherOverridden = mainThreadSurrogate
        mainDispatcherOverridden = mainThreadSurrogate
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }
}