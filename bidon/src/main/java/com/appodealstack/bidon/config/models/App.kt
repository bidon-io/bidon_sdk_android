package com.appodealstack.bidon.config.models

import com.appodealstack.bidon.utils.json.JsonSerializer
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class App(
    var bundle: String,
    var key: String?,
    var framework: String,
    var version: String?,
    var frameworkVersion: String?,
    var pluginVersion: String?
)

internal class AppSerializer : JsonSerializer<App> {
    override fun serialize(data: App): JSONObject {
        return jsonObject {
            "bundle" hasValue data.bundle
            "key" hasValue data.key
            "framework" hasValue data.framework
            "version" hasValue data.version
            "framework_version" hasValue data.frameworkVersion
            "plugin_version" hasValue data.pluginVersion
        }
    }
}