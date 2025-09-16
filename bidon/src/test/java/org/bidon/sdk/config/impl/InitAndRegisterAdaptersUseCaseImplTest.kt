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
    fun `critical adapters initialize synchronously while others run in background`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical1",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background1",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background2",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = mapOf(
                    "critical1" to JSONObject("""{"order": 0}"""),
                    "background1" to JSONObject("""{"order": 1}"""),
                    "background2" to JSONObject("""{"order": 2}""")
                )
            )
        )

        // Critical adapter should be ready
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "critical1"
        )

        delay(300L)

        // All adapters should be ready
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "critical1",
            "background1",
            "background2"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `multiple critical adapters with same order all initialize synchronously`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical1",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "critical2",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background1",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = mapOf(
                    "critical1" to JSONObject("""{"order": 0}"""),
                    "critical2" to JSONObject("""{"order": 0}"""),
                    "background1" to JSONObject("""{"order": 1}""")
                )
            )
        )

        // Both critical adapters should be ready
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "critical1",
            "critical2"
        )

        delay(200L)

        // All adapters should be ready
        assertThat(adaptersSource.adapters.map { it.demandId.demandId }).containsExactly(
            "critical1",
            "critical2",
            "background1"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `failed critical adapters do not prevent SDK from continuing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical_success",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "critical_failure",
                initializationTime = 100L,
                succeedInitialization = false
            ),
            TestInitAdapter(
                name = "background1",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = mapOf(
                    "critical_success" to JSONObject("""{"order": 0}"""),
                    "critical_failure" to JSONObject("""{"order": 0}"""),
                    "background1" to JSONObject("""{"order": 1}""")
                )
            )
        )

        // Only successful critical adapter should be ready
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).contains("critical_success")
        assertThat(initializedAdapters).doesNotContain("critical_failure")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `no adapters configured completes successfully`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = emptyList(),
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = emptyMap()
            )
        )

        // Should complete with no adapters
        assertThat(adaptersSource.adapters).isEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `all adapters with same order are treated as critical`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "adapter1",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter2",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "adapter3",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = mapOf(
                    "adapter1" to JSONObject("""{"order": 5}"""),
                    "adapter2" to JSONObject("""{"order": 5}"""),
                    "adapter3" to JSONObject("""{"order": 5}""")
                )
            )
        )

        // All adapters should be ready since they're all critical
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).containsExactly("adapter1", "adapter2", "adapter3")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `adapters are grouped by order correctly`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "order0_adapter",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "order2_adapter",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "order1_adapter",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 5000,
                adapters = mapOf(
                    "order0_adapter" to JSONObject("""{"order": 0}"""),
                    "order2_adapter" to JSONObject("""{"order": 2}"""),
                    "order1_adapter" to JSONObject("""{"order": 1}""")
                )
            )
        )

        // Only order 0 adapter (critical) should be guaranteed ready
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).contains("order0_adapter")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `critical adapter timeout still allows callback to fire`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical_slow",
                initializationTime = 10000L, // Exceeds timeout
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background_fast",
                initializationTime = 100L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 1000,
                adapters = mapOf(
                    "critical_slow" to JSONObject("""{"order": 0}"""),
                    "background_fast" to JSONObject("""{"order": 1}""")
                )
            )
        )

        // Critical adapter times out, so it's not ready initially
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).isEmpty()

        // Background adapter should still initialize despite critical timeout
        delay(300L)
        val afterBackgroundInit = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(afterBackgroundInit).contains("background_fast")

        // Critical adapter eventually completes after its own timeout
        delay(10000L)
        val finalAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(finalAdapters).containsAtLeast("critical_slow", "background_fast")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `background adapter timeout does not affect other adapters`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical_fast",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background_slow",
                initializationTime = 10000L, // Exceeds timeout
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background_fast",
                initializationTime = 200L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background_medium",
                initializationTime = 500L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 1000,
                adapters = mapOf(
                    "critical_fast" to JSONObject("""{"order": 0}"""),
                    "background_slow" to JSONObject("""{"order": 1}"""),
                    "background_fast" to JSONObject("""{"order": 1}"""),
                    "background_medium" to JSONObject("""{"order": 2}""")
                )
            )
        )

        // Critical adapter should be ready immediately
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).contains("critical_fast")

        // Slow background adapter eventually completes after its own timeout
        delay(10100L)
        val finalAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(finalAdapters).containsAtLeast(
            "critical_fast",
            "background_fast",
            "background_medium",
            "background_slow"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `mixed critical adapter timeouts and successes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        singleDispatcherOverridden = dispatcher
        val adapters = listOf(
            TestInitAdapter(
                name = "critical_fast",
                initializationTime = 100L,
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "critical_slow",
                initializationTime = 10000L, // Exceeds timeout
                succeedInitialization = true
            ),
            TestInitAdapter(
                name = "background1",
                initializationTime = 200L,
                succeedInitialization = true
            )
        )

        testee.invoke(
            context = mockk(relaxed = true),
            adapters = adapters,
            isTestMode = false,
            configResponse = ConfigResponse(
                initializationTimeout = 1000,
                adapters = mapOf(
                    "critical_fast" to JSONObject("""{"order": 0}"""),
                    "critical_slow" to JSONObject("""{"order": 0}"""),
                    "background1" to JSONObject("""{"order": 1}""")
                )
            )
        )

        // Only fast critical adapter should be ready initially
        val initializedAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(initializedAdapters).contains("critical_fast")
        assertThat(initializedAdapters).doesNotContain("critical_slow")

        // Background adapter should still initialize
        delay(400L)
        val afterBackground = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(afterBackground).containsAtLeast("critical_fast", "background1")
        assertThat(afterBackground).doesNotContain("critical_slow")

        // Slow critical adapter eventually completes
        delay(10000L)
        val finalAdapters = adaptersSource.adapters.map { it.demandId.demandId }
        assertThat(finalAdapters).containsAtLeast("critical_fast", "critical_slow", "background1")
    }
}