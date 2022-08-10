package com.appodealstack.bidon.config.domain

import kotlinx.serialization.*

@Serializable
data class AdapterInfo(
    @SerialName("version")
    val adapterVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: String
)
