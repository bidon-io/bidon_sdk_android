package com.appodealstack.applovin

import com.appodealstack.bidon.demands.AdapterParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplovinParameters(
    @SerialName("applovin_key")
    val key: String
): AdapterParameters