package com.appodealstack.fyber

import com.appodealstack.bidon.demands.AdapterParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FairBidParameters(
    @SerialName("app_key")
    val appKey: String,
) : AdapterParameters