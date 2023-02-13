package com.appodealstack.bidon.base

import com.appodealstack.bidon.utils.defaultDispatcherOverridden
import com.appodealstack.bidon.utils.ioDispatcherOverridden
import com.appodealstack.bidon.utils.mainDispatcherOverridden
import com.appodealstack.bidon.utils.singleDispatcherOverridden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
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