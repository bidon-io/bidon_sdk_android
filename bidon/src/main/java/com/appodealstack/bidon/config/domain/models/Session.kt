package com.appodealstack.bidon.config.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    @SerialName("id")
    var id: String,
    @SerialName("launch_ts")
    var launchTs: Long,
    @SerialName("launch_monotonic_ts")
    var launchMonotonicTs: Long,
    @SerialName("start_ts")
    var startTs: Long,
    @SerialName("monotonic_start_ts")
    var monotonicStartTs: Long,
    @SerialName("ts")
    var ts: Long,
    @SerialName("monotonic_ts")
    var monotonicTs: Long,
    @SerialName("memory_warnings_ts")
    var memoryWarningsTs: List<Long> = arrayListOf(),
    @SerialName("memory_warnings_monotonic_ts")
    var memoryWarningsMonotonicTs: List<Long> = arrayListOf(),
    @SerialName("ram_used")
    var ramUsed: Long,
    @SerialName("ram_size")
    var ramSize: Long,
    @SerialName("storage_free")
    var storageFree: Long,
    @SerialName("storage_used")
    var storageUsed: Long,
    @SerialName("battery")
    var battery: Float,
    @SerialName("cpu_usage")
    var cpuUsage: Float
)