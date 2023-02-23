package org.bidon.sdk.databinders.app

import org.bidon.sdk.config.models.App
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.json.JsonParsers
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