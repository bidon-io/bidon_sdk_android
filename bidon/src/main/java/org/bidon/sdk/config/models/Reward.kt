package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

internal data class Reward(
    @field:JsonName("title")
    val title: String,
    @field:JsonName("value")
    val amount: Int,
) : Serializable
