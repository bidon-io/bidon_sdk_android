package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class Session(
    @field:JsonName("id")
    var id: String,
    @field:JsonName("launch_ts")
    var launchTs: Long,
    @field:JsonName("launch_monotonic_ts")
    var launchMonotonicTs: Long,
    @field:JsonName("start_ts")
    var startTs: Long,
    @field:JsonName("start_monotonic_ts")
    var monotonicStartTs: Long,
    @field:JsonName("ts")
    var ts: Long,
    @field:JsonName("monotonic_ts")
    var monotonicTs: Long,
    @field:JsonName("memory_warnings_ts")
    var memoryWarningsTs: List<Long>,
    @field:JsonName("memory_warnings_monotonic_ts")
    var memoryWarningsMonotonicTs: List<Long>,
    @field:JsonName("ram_used")
    var ramUsed: Long,
    @field:JsonName("ram_size")
    var ramSize: Long,
    @field:JsonName("storage_free")
    var storageFree: Long,
    @field:JsonName("storage_used")
    var storageUsed: Long,
    @field:JsonName("battery")
    var battery: Float,
    @field:JsonName("cpu_usage")
    var cpuUsage: Float
) : Serializable
