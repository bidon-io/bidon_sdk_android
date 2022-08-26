package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.config.domain.models.App
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.app.AppDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

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