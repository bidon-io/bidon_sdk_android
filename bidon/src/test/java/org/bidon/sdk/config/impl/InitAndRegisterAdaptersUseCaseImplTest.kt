package org.bidon.sdk.config.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.impl.AdaptersSourceImpl
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.mockkLog
import org.bidon.sdk.utils.singleDispatcherOverridden
import org.json.JSONObject
import org.junit.Before
import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 28/11/2023.
 */
class InitAndRegisterAdaptersUseCaseImplTest {

    private val adaptersSource: AdaptersSource = AdaptersSourceImpl()

    private val testee: InitAndRegisterAdaptersUseCase by lazy {
        InitAndRegisterAdaptersUseCaseImpl(adaptersSource)
    }

    @Before
    fun setUp() {
        mockkLog()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `it should successfully init adapters`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "adapter1",
                initializationTime = 1000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter2",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter3",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter4",
                initializationTime = 1000L,
                succeedInitialization = true
            ),
        )
        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 15000,
                adapters = mapOf(
                    "adapter1" to JSONObject("""{"order": 0}"""),
                    "adapter2" to JSONObject("""{"order": 1}"""),
                    "adapter3" to JSONObject("""{"order": 1}"""),
                    "adapter4" to JSONObject("""{"order": 2}""")
                )
            )
        )
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter4",
            "adapter3",
            "adapter2",
            "adapter1",
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `it should successfully init adapters after timed out`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "adapter1",
                initializationTime = 1000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter2",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter3",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter4",
                initializationTime = 20000L,
                succeedInitialization = true
            ),
        )
        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 15000,
                adapters = mapOf(
                    "adapter1" to JSONObject("""{"order": 0}"""),
                    "adapter2" to JSONObject("""{"order": 1}"""),
                    "adapter3" to JSONObject("""{"order": 1}"""),
                    "adapter4" to JSONObject("""{"order": 2}""")
                )
            )
        )
        // while timeout (15000) it should initialize only 3 adapters
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter3",
            "adapter2",
            "adapter1",
        )

        // after timeout (15000) it should initialize 4th adapter
        delay(20000)
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter4",
            "adapter3",
            "adapter2",
            "adapter1",
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `it should successfully init 2 adapters and 2 others after timed out`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "adapter1",
                initializationTime = 1000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter2",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter3",
                initializationTime = 16000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter4",
                initializationTime = 30000L,
                succeedInitialization = true
            ),
        )
        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 15000,
                adapters = mapOf(
                    "adapter1" to JSONObject("""{"order": 0}"""),
                    "adapter2" to JSONObject("""{"order": 1}"""),
                    "adapter3" to JSONObject("""{"order": 1}"""),
                    "adapter4" to JSONObject("""{"order": 2}""")
                )
            )
        )
        // while timeout (15000) it should initialize only 2 adapters
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter2",
            "adapter1",
        )

        // after timeout (15000) it should initialize 4th adapter
        delay(20000)
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter3",
            "adapter2",
            "adapter1",
        )
        delay(20000)
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter4",
            "adapter3",
            "adapter2",
            "adapter1",
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `it should skip failed adapters`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "adapter1",
                initializationTime = 1000L,
                succeedInitialization = false
            ),
            TestInitAdapter(
                name = "adapter2",
                initializationTime = 2000L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter3",
                initializationTime = 16000L,
                succeedInitialization = false
            ),
            TestInitAdapter(
                name = "adapter4",
                initializationTime = 30000L,
                succeedInitialization = true
            ),
        )
        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 15000,
                adapters = mapOf(
                    "adapter1" to JSONObject("""{"order": 0}"""),
                    "adapter2" to JSONObject("""{"order": 1}"""),
                    "adapter3" to JSONObject("""{"order": 1}"""),
                    "adapter4" to JSONObject("""{"order": 2}""")
                )
            )
        )
        // while timeout (15000) it should initialize only 2 adapters
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter2",
        )

        // after timeout (15000) it should initialize 4th adapter
        delay(20000)
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter2",
        )
        delay(20000)
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "adapter4",
            "adapter2",
        )
    }
}