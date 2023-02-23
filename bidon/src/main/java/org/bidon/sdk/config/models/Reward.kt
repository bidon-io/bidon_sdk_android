package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

data class Reward(
    @field:JsonName("title")
    val currency: String,
    @field:JsonName("value")
    val amount: Int,
): Serializable
