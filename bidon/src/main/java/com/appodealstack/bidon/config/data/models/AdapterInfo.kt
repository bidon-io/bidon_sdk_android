package com.appodealstack.bidon.config.data.models

import kotlinx.serialization.*

@Serializable
data class AdapterInfo(
    @SerialName("version")
    val adapterVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: String
)
