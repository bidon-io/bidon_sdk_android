package com.appodealstack.bidon.config.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigRequestBody(
    @SerialName("adapters")
    val adapters: List<AdapterInfo>
)