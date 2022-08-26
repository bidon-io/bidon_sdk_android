package com.appodealstack.bidon.config.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Placement(
    @SerialName("name")
    val name: String,
    @SerialName("reward")
    val reward: Reward?,
    @SerialName("capping")
    val capping: Capping?,
)

@Serializable
data class Reward(
    @SerialName("title")
    val currency: String,
    @SerialName("value")
    val amount: Int,
)

//TODO clarify model
@Serializable
data class Capping(
    @SerialName("setting")
    val setting: String,
    @SerialName("value")
    val value: Int,
)