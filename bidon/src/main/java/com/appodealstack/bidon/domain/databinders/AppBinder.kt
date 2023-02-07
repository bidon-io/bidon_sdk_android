package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.app.AppDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.App
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AppBinder(
    private val dataSource: AppDataSource,
) : DataBinder {
    override val fieldName: String = "app"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createApp())

    private fun createApp(): App {
        return App(
            bundle = dataSource.getBundleId(),
            key = dataSource.getAppKey(),
            framework = dataSource.getFramework(),
            version = dataSource.getVersion(),
            frameworkVersion = dataSource.getFrameworkVersion(),
            pluginVersion = dataSource.getPluginVersion()
        )
    }
}