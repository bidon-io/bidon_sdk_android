package com.appodealstack.ironsource

import com.appodealstack.bidon.domain.adapter.AdapterParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IronSourceParameters(
    @SerialName("app_key")
    val appKey: String,
) : AdapterParameters