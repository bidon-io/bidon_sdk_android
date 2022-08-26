package com.appodealstack.bidon.config.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    @SerialName("ua")
    val userAgent: String?,
    @SerialName("make")
    val manufacturer: String?,
    @SerialName("model")
    val deviceModel: String?,
    @SerialName("os")
    val os: String?,
    @SerialName("osv")
    val osVersion: String?,
    @SerialName("hwv")
    val hardwareVersion: String?,

    @SerialName("h")
    val height: Int?,
    @SerialName("w")
    val width: Int?,
    @SerialName("ppi")
    val ppi: Int?,

    @SerialName("pxratio")
    val pxRatio: Float?,
    @SerialName("js")
    val javaScriptSupport: Int?,

    @SerialName("language")
    val language: String?,
    @SerialName("carrier")
    val carrier: String?,
    @SerialName("mccmnc")
    val mccmnc: String?,
    @SerialName("connection_type")
    val connectionType: Int?,
)