package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    @SerialName("token")
    val token: String
)
