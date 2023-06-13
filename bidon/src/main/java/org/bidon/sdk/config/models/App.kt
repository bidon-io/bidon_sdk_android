package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class App(
    @field:JsonName("bundle")
    var bundle: String,
    @field:JsonName("key")
    var key: String?,
    @field:JsonName("framework")
    var framework: String,
    @field:JsonName("version")
    var version: String?,
    @field:JsonName("framework_version")
    var frameworkVersion: String?,
    @field:JsonName("plugin_version")
    var pluginVersion: String?,
    @field:JsonName("bidon_version")
    var bidonVersion: String
) : Serializable
