package com.appodealstack.bidon.config.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    @SerialName("token")
    val token: String
)
