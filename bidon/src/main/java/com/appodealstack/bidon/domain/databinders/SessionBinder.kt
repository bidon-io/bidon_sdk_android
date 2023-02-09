package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.session.SessionDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.config.Session
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class SessionBinder(
    private val dataSource: SessionDataSource
) : DataBinder<JSONObject>  {
    override val fieldName: String = "session"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createSession())

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