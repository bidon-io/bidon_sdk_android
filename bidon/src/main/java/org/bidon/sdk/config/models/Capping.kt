package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

// TODO clarify model
data class Capping(
    @field:JsonName("setting")
    val setting: String,
    @field:JsonName("value")
    val value: Int,
) : Serializable
