package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class Session(
    var id: String,
    var launchTs: Long,
    var launchMonotonicTs: Long,
    var startTs: Long,
    var monotonicStartTs: Long,
    var ts: Long,
    var monotonicTs: Long,
    var memoryWarningsTs: List<Long>,
    var memoryWarningsMonotonicTs: List<Long>,
    var ramUsed: Long,
    var ramSize: Long,
    var storageFree: Long,
    var storageUsed: Long,
    var battery: Float,
    var cpuUsage: Float
)

internal class SessionSerializer : JsonSerializer<Session> {
    override fun serialize(data: Session): JSONObject {
        return jsonObject {
            "id" hasValue data.id
            "launch_ts" hasValue data.launchTs
            "launch_monotonic_ts" hasValue data.launchMonotonicTs
            "start_ts" hasValue data.startTs
            "start_monotonic_ts" hasValue data.monotonicStartTs
            "ts" hasValue data.ts
            "monotonic_ts" hasValue data.monotonicTs
            "memory_warnings_ts" hasValue data.memoryWarningsTs
            "memory_warnings_monotonic_ts" hasValue data.memoryWarningsMonotonicTs
            "ram_used" hasValue data.ramUsed
            "ram_size" hasValue data.ramSize
            "storage_free" hasValue data.storageFree
            "storage_used" hasValue data.storageUsed
            "battery" hasValue data.battery
            "cpu_usage" hasValue data.cpuUsage
        }
    }
}