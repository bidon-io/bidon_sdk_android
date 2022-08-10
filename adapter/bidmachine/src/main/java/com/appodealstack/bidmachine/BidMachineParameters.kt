package com.appodealstack.bidmachine

import com.appodealstack.bidon.adapters.AdapterParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BidMachineParameters(
    @SerialName("seller_id")
    val sellerId: String,
    @SerialName("endpoint")
    val endpoint: String?,
    @SerialName("mediation_config")
    val mediationConfig: List<String>?,
): AdapterParameters