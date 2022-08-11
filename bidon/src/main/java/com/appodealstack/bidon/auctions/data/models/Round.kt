package com.appodealstack.bidon.auctions.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Round(
    @SerialName("id")
    val id: String,
    @SerialName("timeout")
    val timeoutMs: Long,
    @SerialName("demands")
    val demandIds: List<String>
)