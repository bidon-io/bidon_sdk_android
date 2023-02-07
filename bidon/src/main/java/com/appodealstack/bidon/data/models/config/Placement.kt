package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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

// TODO clarify model
@Serializable
data class Capping(
    @SerialName("setting")
    val setting: String,
    @SerialName("value")
    val value: Int,
)