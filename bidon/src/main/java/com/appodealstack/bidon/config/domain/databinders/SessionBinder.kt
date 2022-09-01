package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.data.models.Session
import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.session.SessionDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class SessionBinder(
    private val dataSource: SessionDataSource
) : DataBinder {
    override val fieldName: String = "session"

    override suspend fun getJsonElement(): JsonElement =
        BidonJson.encodeToJsonElement(createSession())

    private fun createSession(): Session {
        return Session(
            id = dataSource.getId(),
            launchTs = dataSource.getLaunchTs(),
            launchMonotonicTs = dataSource.getLaunchMonotonicTs(),
            startTs = dataSource.getStartTs(),
            monotonicStartTs = dataSource.getMonotonicStartTs(),
            ts = dataSource.getTs(),
            monotonicTs = dataSource.getMonotonicTs(),
            memoryWarningsTs = dataSource.getMemoryWarningsTs(),
            memoryWarningsMonotonicTs = dataSource.getMemoryWarningsMonotonicTs(),
            ramUsed = dataSource.getRamUsed(),
            ramSize = dataSource.getRamSize(),
            storageFree = dataSource.getStorageFree(),
            storageUsed = dataSource.getStorageUsed(),
            battery = dataSource.getBattery(),
            cpuUsage = dataSource.getCpuUsage()
        )
    }
}