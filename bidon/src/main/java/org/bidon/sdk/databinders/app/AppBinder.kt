package org.bidon.sdk.databinders.app

import org.bidon.sdk.config.models.App
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AppBinder(
    private val dataSource: AppDataSource,
) : DataBinder<JSONObject> {
    override val fieldName: String = "app"

    override suspend fun getJsonObject(): JSONObject = createApp().serialize()

    private fun createApp(): App {
        return App(
            bundle = dataSource.getBundleId(),
            key = dataSource.getAppKey(),
            framework = dataSource.getFramework(),
            version = dataSource.getAppVersionName(),
            frameworkVersion = dataSource.getFrameworkVersion(),
            pluginVersion = dataSource.getPluginVersion(),
            bidonVersion = dataSource.getBidonVersion()
        )
    }
}