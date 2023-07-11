package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.json.jsonArray
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONArray
import org.junit.Test

/**
 * Created by Bidon Team on 23/02/2023.
 */
internal class SessionSerializerTest {

    @Test
    fun `session serialize`() {
        val session = Session(
            id = "6b8768da-6d8b-416e-9925-8bfe5310d649",
            launchTs = 1677172149700,
            launchMonotonicTs = 402181881,
            startTs = 1677172149700,
            monotonicStartTs = 402181881,
            ts = 1677172150067,
            monotonicTs = 402182248,
            memoryWarningsTs = listOf(1, 2, 3),
            memoryWarningsMonotonicTs = listOf(),
            ramUsed = 182131712,
            ramSize = 5677834240,
            storageFree = 61220335616,
            storageUsed = 51384143872,
            battery = 100.0f,
            cpuUsage = 0.8749058f
        )
        val actual = session.serialize()
        actual.assertEquals(
            expectedJsonStructure {
                "start_ts" hasValue 1677172149700
                "storage_free" hasValue 61220335616
                "launch_ts" hasValue 1677172149700
                "memory_warnings_ts" hasArray jsonArray {
                    putValues(listOf(1, 2, 3))
                }
                "ram_used" hasValue 182131712
                "battery" hasValue 100
                "ram_size" hasValue 5677834240
                "start_monotonic_ts" hasValue 402181881
                "launch_monotonic_ts" hasValue 402181881
                "memory_warnings_monotonic_ts" hasArray JSONArray()
                "monotonic_ts" hasValue 402182248
                "id" hasValue "6b8768da-6d8b-416e-9925-8bfe5310d649"
                "cpu_usage" hasValue 0.8749058
                "storage_used" hasValue 51384143872
                "ts" hasValue 1677172150067
            }
        )
    }
}