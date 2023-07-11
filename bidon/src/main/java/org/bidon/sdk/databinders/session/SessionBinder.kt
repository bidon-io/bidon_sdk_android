package org.bidon.sdk.databinders.session

import org.bidon.sdk.config.models.Session
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class SessionBinder(
    private val dataSource: SessionDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "session"

    override suspend fun getJsonObject(): JSONObject = createSession().serialize()

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