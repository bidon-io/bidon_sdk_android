package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class Device(
    @field:JsonName("ua")
    val userAgent: String?,
    @field:JsonName("make")
    val manufacturer: String?,
    @field:JsonName("model")
    val deviceModel: String?,
    @field:JsonName("os")
    val os: String?,
    @field:JsonName("osv")
    val osVersion: String?,
    @field:JsonName("os_api_level")
    val osApiLevel: String?,
    @field:JsonName("hwv")
    val hardwareVersion: String?,
    @field:JsonName("h")
    val height: Int?,
    @field:JsonName("w")
    val width: Int?,
    @field:JsonName("ppi")
    val ppi: Int?,
    @field:JsonName("pxratio")
    val pxRatio: Float?,
    @field:JsonName("js")
    val javaScriptSupport: Int?,
    @field:JsonName("language")
    val language: String?,
    @field:JsonName("carrier")
    val carrier: String?,
    @field:JsonName("mccmnc")
    val mccmnc: String?,
    @field:JsonName("connection_type")
    val connectionType: String?,
    @field:JsonName("type")
    val type: String,
) : Serializable
