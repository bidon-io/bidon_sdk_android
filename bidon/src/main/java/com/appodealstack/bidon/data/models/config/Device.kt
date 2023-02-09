package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class Device(
    val userAgent: String?,
    val manufacturer: String?,
    val deviceModel: String?,
    val os: String?,
    val osVersion: String?,
    val hardwareVersion: String?,
    val height: Int?,
    val width: Int?,
    val ppi: Int?,
    val pxRatio: Float?,
    val javaScriptSupport: Int?,
    val language: String?,
    val carrier: String?,
    val mccmnc: String?,
    val connectionType: String?,
)

internal class DeviceSerializer : JsonSerializer<Device> {
    override fun serialize(data: Device): JSONObject {
        return jsonObject {
            "ua" hasValue data.userAgent
            "make" hasValue data.manufacturer
            "model" hasValue data.deviceModel
            "os" hasValue data.os
            "osv" hasValue data.osVersion
            "hwv" hasValue data.hardwareVersion
            "h" hasValue data.height
            "w" hasValue data.width
            "ppi" hasValue data.ppi
            "pxratio" hasValue data.pxRatio
            "js" hasValue data.javaScriptSupport
            "language" hasValue data.language
            "carrier" hasValue data.carrier
            "mccmnc" hasValue data.mccmnc
            "connection_type" hasValue data.connectionType
        }
    }
}