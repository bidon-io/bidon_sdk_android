package com.appodealstack.bidon.config.domain.databinders

import android.os.Build
import com.appodealstack.bidon.BuildConfig
import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.core.BidonJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class AppBinder : DataBinder {
    override val fieldName: String = "app"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createApp())

    private fun createApp(): App = App(
        key = "b1689e101a2555084e08c2ba7375783bde166625bbeae00f",
        bundle = "com.bidon.demo",
        framework = "Android",
        frameworkVersion = "32",
        version = "Android 11",
        pluginVersion = "0.0.1-alpha2"
    )
}

@Serializable
internal data class App(
    @SerialName("key")
    val key: String,
    @SerialName("bundle")
    val bundle: String,
    @SerialName("framework")
    val framework: String,
    @SerialName("version")
    val version: String,
    @SerialName("framework_version")
    val frameworkVersion: String,
    @SerialName("plugin_version")
    val pluginVersion: String,
)
