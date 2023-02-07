package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.session.SessionDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.Session
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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