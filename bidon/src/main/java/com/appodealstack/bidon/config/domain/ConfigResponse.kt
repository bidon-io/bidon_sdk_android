package com.appodealstack.bidon.config.domain

import kotlinx.serialization.SerialName

data class ConfigResponse(
    @SerialName("tmax")
    val initializationTimeout: Long,
    @SerialName("adapters")
    val adapters: List<AdapterInitializationInfo>
)