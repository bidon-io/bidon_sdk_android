package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.app.AppDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.config.App
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AppBinder(
    private val dataSource: AppDataSource,
) : DataBinder<JSONObject> {
    override val fieldName: String = "app"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createApp())

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