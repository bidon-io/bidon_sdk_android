package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.*

@Serializable
data class AdapterInfo(
    @SerialName("version")
    val adapterVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: String
)
