package com.appodealstack.bidon.base

import com.appodealstack.bidon.core.defaultDispatcherOverridden
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.core.ioDispatcherOverridden
import com.appodealstack.bidon.core.mainDispatcherOverridden
import com.appodealstack.bidon.core.singleDispatcherOverridden
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before

/**
 * @see https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
 *
 *
 *
 */
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

//    @Test
//    fun testFoo() = runTest {
//        launch {
//            println(1)   // executes during runCurrent()
//            delay(1_000) // suspends until time is advanced by at least 1_000
//            println(2)   // executes during advanceTimeBy(2_000)
//            delay(500)   // suspends until the time is advanced by another 500 ms
//            println(3)   // also executes during advanceTimeBy(2_000)
//            delay(5_000) // will suspend by another 4_500 ms
//            println(4)   // executes during advanceUntilIdle()
//        }
//        // the child coroutine has not run yet
//        runCurrent()
//        // the child coroutine has called println(1), and is suspended on delay(1_000)
//        advanceTimeBy(2_000) // progress time, this will cause two calls to `delay` to resume
//        // the child coroutine has called println(2) and println(3) and suspends for another 4_500 virtual milliseconds
//        advanceUntilIdle() // will run the child coroutine to completion
//        assertEquals(6500, currentTime) // the child coroutine finished at virtual time of 6_500 milliseconds
//    }
}