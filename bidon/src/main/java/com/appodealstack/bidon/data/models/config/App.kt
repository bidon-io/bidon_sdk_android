package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
data class App(
    @SerialName("bundle")
    var bundle: String,
    @SerialName("key")
    var key: String?,
    @SerialName("framework")
    var framework: String,
    @SerialName("version")
    var version: String?,
    @SerialName("framework_version")
    var frameworkVersion: String?,
    @SerialName("plugin_version")
    var pluginVersion: String?
)