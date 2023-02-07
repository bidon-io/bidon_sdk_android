package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.*
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
data class AdapterInfo(
    @SerialName("version")
    val adapterVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: String
)
