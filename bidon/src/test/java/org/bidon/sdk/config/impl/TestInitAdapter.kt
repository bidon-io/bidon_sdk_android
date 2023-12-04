package org.bidon.sdk.config.impl

import android.content.Context
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 28/11/2023.
 */
internal class TestInitAdapter(
    private val name: String,
    private val initializationTime: Long,
    private val succeedInitialization: Boolean,
) : Adapter,
    Initializable<TestInitAdapterParameters> {
    override val demandId: DemandId = DemandId(name)
    override val adapterInfo: AdapterInfo =
        AdapterInfo(adapterVersion = "${name}AdapterVersion1", sdkVersion = "${name}SdkVersion1")

    override suspend fun init(context: Context, configParams: TestInitAdapterParameters) = coroutineScope {
        delay(initializationTime)
        suspendCoroutine {
            if (succeedInitialization) {
                it.resume(Unit)
            } else {
                it.resumeWithException(RuntimeException("Test initialization failed $demandId"))
            }
        }
    }

    override fun parseConfigParam(json: String): TestInitAdapterParameters {
        return TestInitAdapterParameters(
            initializationTime = initializationTime,
            succeedInitialization = succeedInitialization
        )
    }
}

internal data class TestInitAdapterParameters(
    val initializationTime: Long,
    val succeedInitialization: Boolean
) : AdapterParameters